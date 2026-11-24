package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import org.xml.sax.InputSource;
import org.w3c.dom.Element;

/** Adapter SOAP 1.1 document/literal. No depende de clases del proyecto externo. */
@Component
public class ProveedorSoapAdapter implements IProveedorGateway {
    private static final String NS = "urn:aurea:proveedor:v1";
    private static final String SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    /** Tras 3 llamadas seguidas sin respuesta, deja de llamar al proveedor durante 30 s. */
    static final int FALLOS_PARA_ABRIR = 3;
    static final Duration ESPERA_CIRCUITO_ABIERTO = Duration.ofSeconds(30);
    private final CircuitBreaker circuito = new CircuitBreaker("proveedor-soap", FALLOS_PARA_ABRIR, ESPERA_CIRCUITO_ABIERTO);
    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;
    public ProveedorSoapAdapter(@Value("${app.proveedor.url:http://127.0.0.1:8091/soap/proveedor}") String url,
            @Value("${app.proveedor.timeout-ms:5000}") long timeoutMs) {
        endpoint = URI.create(url);
        timeout = Duration.ofMillis(timeoutMs);
        client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }
    public ResultadoProveedorDTO disponibilidad(String codigo) {
        return invocar("consultarDisponibilidad", campo("codigoProducto", codigo));
    }
    public ResultadoProveedorDTO solicitar(String id, String codigo, int cantidad) {
        return invocar("solicitarReposicion", campo("idSolicitud", id) + campo("codigoProducto", codigo) + campo("cantidad", String.valueOf(cantidad)));
    }
    public ResultadoProveedorDTO consultar(String id) {
        return invocar("consultarEstado", campo("idSolicitud", id));
    }
    /** Estado del circuito hacia el proveedor (para pruebas y diagnóstico). */
    public CircuitBreaker.Estado estadoCircuito() {
        return circuito.estado();
    }
    private ResultadoProveedorDTO invocar(String operacion, String campos) {
        long espera = circuito.permitir();
        if (espera > 0) {
            // Fallback: la reposición queda SOLICITADA con este aviso y se reintenta luego con el mismo identificador.
            throw new ProveedorNoDisponibleException("El proveedor no está respondiendo, así que no se lo consultó. "
                    + "La solicitud quedó guardada y se puede reintentar en " + espera
                    + " segundos sin duplicarla.", null, espera);
        }
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><s:Envelope xmlns:s=\"" + SOAP
                + "\"><s:Body><p:" + operacion + " xmlns:p=\"" + NS + "\">" + campos
                + "</p:" + operacion + "></s:Body></s:Envelope>";
        HttpResponse<String> response;
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "\"\"")
                    .POST(HttpRequest.BodyPublishers.ofString(xml)).build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            circuito.registrarFallo();
            Thread.currentThread().interrupt();
            throw new ProveedorNoDisponibleException("Consulta al proveedor interrumpida", ex);
        } catch (java.io.IOException ex) {
            circuito.registrarFallo();
            throw new ProveedorNoDisponibleException("El proveedor no respondió a tiempo. La solicitud quedó guardada; "
                    + "puede reintentar sin duplicarla.", ex);
        } catch (RuntimeException ex) {
            circuito.registrarFallo();
            throw new ProveedorNoDisponibleException("No se pudo contactar al proveedor. Puede reintentar sin duplicar la solicitud.", ex);
        }
        // Un SOAP Fault de negocio viaja con HTTP 500 pero es una respuesta válida: no abre el circuito.
        String cuerpo = response.body() == null ? "" : response.body();
        if (response.statusCode() >= 500 && !cuerpo.contains("Fault>")) {
            circuito.registrarFallo();
        } else {
            circuito.registrarExito();
        }
        try {
            if (response.body().length() > 65536) throw new IllegalArgumentException("Respuesta excesiva");
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.body())));
            if (document.getElementsByTagNameNS(SOAP, "Fault").getLength() > 0)
                throw new ProveedorNoDisponibleException("El proveedor devolvió un SOAP Fault. Revise el código o reintente con la misma solicitud.", null);
            if (response.statusCode() != 200
                    || document.getElementsByTagNameNS(NS, operacion + "Response").getLength() != 1)
                throw new IllegalArgumentException("Contrato SOAP inesperado");
            var resultados = document.getElementsByTagNameNS("*", "resultado");
            if (resultados.getLength() != 1) throw new IllegalArgumentException("Resultado SOAP inválido");
            Element resultado = (Element) resultados.item(0);
            return new ResultadoProveedorDTO(texto(resultado, "idSolicitud"), texto(resultado, "codigoProducto"),
                    Integer.parseInt(texto(resultado, "cantidad")), texto(resultado, "estado"),
                    Integer.parseInt(texto(resultado, "disponibles")));
        } catch (ProveedorNoDisponibleException ex) { throw ex; }
        catch (Exception ex) {
            throw new ProveedorNoDisponibleException("No se pudo obtener una respuesta válida del proveedor. Puede reintentar sin duplicar la solicitud.", ex);
        }
    }
    private static String texto(Element e, String nombre) {
        var nodes = e.getElementsByTagNameNS("*", nombre);
        if (nodes.getLength() != 1) throw new IllegalArgumentException("Falta campo SOAP: " + nombre);
        return nodes.item(0).getTextContent();
    }
    private static String campo(String nombre, String valor) {
        if (valor == null) throw new IllegalArgumentException("Campo obligatorio");
        String escaped = valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
        return "<" + nombre + ">" + escaped + "</" + nombre + ">";
    }
}
