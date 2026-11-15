package com.joyeriaEcommerce.AureaTPO.reposiciones;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class ProveedorSoapAdapterTests {
    HttpServer server;
    String response;
    String request;
    ProveedorSoapAdapter adapter;
    @BeforeEach void iniciar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        adapter = new ProveedorSoapAdapter("http://127.0.0.1:" + server.getAddress().getPort(), 500);
    }
    @AfterEach void cerrar() { server.stop(0); }
    @Test void interpretaContratoYEscapaEntrada() {
        response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
            + "<p:consultarDisponibilidadResponse xmlns:p=\"urn:aurea:proveedor:v1\"><resultado>"
            + "<idSolicitud></idSolicitud><codigoProducto>ANILLO-PLATA</codigoProducto><cantidad>0</cantidad>"
            + "<estado>DISPONIBLE</estado><disponibles>12</disponibles>"
            + "</resultado></p:consultarDisponibilidadResponse></s:Body></s:Envelope>";
        assertThat(adapter.disponibilidad("A&B").disponibles()).isEqualTo(12);
        assertThat(request).contains("A&amp;B", "consultarDisponibilidad");
    }
    @Test void rechazaDtdYEntidadesExternas() {
        response = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY xxe SYSTEM \"file:///no-leer\">]><x>&xxe;</x>";
        assertThatThrownBy(() -> adapter.disponibilidad("A")).isInstanceOf(ProveedorNoDisponibleException.class);
    }
    @Test void traduceSoapFaultSinConfundirloConAceptacion() {
        response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault><faultstring>Error</faultstring></s:Fault></s:Body></s:Envelope>";
        assertThatThrownBy(() -> adapter.disponibilidad("A")).isInstanceOf(ProveedorNoDisponibleException.class).hasMessageContaining("SOAP Fault");
    }
}
