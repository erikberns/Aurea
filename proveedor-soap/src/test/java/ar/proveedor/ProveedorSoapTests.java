package ar.proveedor;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.xml.ws.Endpoint;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"proveedor.soap.enabled=false", "spring.datasource.url=jdbc:h2:mem:proveedor-test", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ProveedorSoapTests {
    @Autowired ProveedorService service;
    HttpServer server;
    Endpoint endpoint;
    String url;
    @BeforeEach void iniciar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        endpoint = Endpoint.create(new ProveedorEndpoint(service));
        endpoint.publish(server.createContext("/soap/proveedor"));
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/soap/proveedor";
    }
    @AfterEach void cerrar() { endpoint.stop(); server.stop(0); }
    @Test void publicaWsdlYRespondeSoapReal() throws Exception {
        var wsdl = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url + "?wsdl")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(wsdl.statusCode()).isEqualTo(200);
        assertThat(wsdl.body()).contains("solicitarReposicion", "consultarDisponibilidad", "consultarEstado", "soap:binding");
        var response = soap("consultarDisponibilidad", "<codigoProducto>ANILLO-PLATA</codigoProducto>");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("DISPONIBLE", "ANILLO-PLATA");
    }
    @Test void solicitudDuplicadaNoVuelveADescontarYSePuedeConsultar() throws Exception {
        int antes = service.disponibilidad("COLLAR-ORO").disponibles;
        String id = UUID.randomUUID().toString();
        String campos = "<idSolicitud>" + id + "</idSolicitud><codigoProducto>COLLAR-ORO</codigoProducto><cantidad>3</cantidad>";
        assertThat(soap("solicitarReposicion", campos).body()).contains("ACEPTADA");
        assertThat(soap("solicitarReposicion", campos).body()).contains("ACEPTADA");
        assertThat(service.disponibilidad("COLLAR-ORO").disponibles).isEqualTo(antes - 3);
        assertThat(soap("consultarEstado", "<idSolicitud>" + id + "</idSolicitud>").body()).contains("ACEPTADA", id);
        assertThat(soap("solicitarReposicion", campos.replace("<cantidad>3", "<cantidad>4")).statusCode()).isEqualTo(500);
    }
    @Test void rechazaPorFaltaDeStockSinDescontar() {
        int antes = service.disponibilidad("PULSERA-PLATA").disponibles;
        var r = service.solicitar(UUID.randomUUID().toString(), "PULSERA-PLATA", 10000);
        assertThat(r.estado).isEqualTo("RECHAZADA");
        assertThat(service.disponibilidad("PULSERA-PLATA").disponibles).isEqualTo(antes);
    }
    @Test void devuelveSoapFaultParaCodigoDesconocido() throws Exception {
        var r = soap("consultarDisponibilidad", "<codigoProducto>INEXISTENTE</codigoProducto>");
        assertThat(r.statusCode()).isEqualTo(500);
        assertThat(r.body()).contains("Fault", "Producto desconocido");
    }
    private HttpResponse<String> soap(String op, String campos) throws Exception {
        String body = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><p:" + op
                + " xmlns:p=\"urn:aurea:proveedor:v1\">" + campos + "</p:" + op + "></s:Body></s:Envelope>";
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
