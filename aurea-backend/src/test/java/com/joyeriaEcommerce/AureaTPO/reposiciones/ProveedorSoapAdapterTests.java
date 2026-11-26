package com.joyeriaEcommerce.AureaTPO.reposiciones;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.*;
import com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia.CircuitBreaker;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class ProveedorSoapAdapterTests {
    HttpServer server;
    String response;
    String request;
    volatile long demoraMs;
    final AtomicInteger llamadas = new AtomicInteger();
    ProveedorSoapAdapter adapter;
    @BeforeEach void iniciar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            llamadas.incrementAndGet();
            if (demoraMs > 0) {
                try { Thread.sleep(demoraMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        // Un hilo por solicitud: con el ejecutor por defecto (uno solo), las solicitudes que el cliente ya
        // abandonó por timeout quedaban en cola y el contador no las veía a tiempo.
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
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
    private static final String FAULT = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault><faultstring>Error</faultstring></s:Fault></s:Body></s:Envelope>";
    @Test void trasTresTimeoutsElCircuitoSeAbreYRespondeAlInstanteSinLlamarAlProveedor() {
        response = FAULT;
        demoraMs = 1500; // el adapter usa un timeout de 500 ms
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> adapter.consultar("id")).isInstanceOf(ProveedorNoDisponibleException.class)
                    .hasMessageContaining("no respondió a tiempo");
        }
        assertThat(adapter.estadoCircuito()).isEqualTo(CircuitBreaker.Estado.ABIERTO);
        long inicio = System.nanoTime();
        assertThatThrownBy(() -> adapter.consultar("id")).isInstanceOfSatisfying(ProveedorNoDisponibleException.class,
                ex -> {
                    assertThat(ex.getSegundosParaReintentar()).isPositive();
                    assertThat(ex.getMessage()).contains("se puede reintentar");
                });
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(java.time.Duration.ofMillis(200));
        assertThat(llamadas.get()).as("con el circuito abierto no se vuelve a llamar").isEqualTo(3);
    }
    @Test void unSoapFaultDeNegocioNoAbreElCircuito() {
        response = FAULT;
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> adapter.disponibilidad("A")).isInstanceOf(ProveedorNoDisponibleException.class);
        }
        assertThat(adapter.estadoCircuito()).isEqualTo(CircuitBreaker.Estado.CERRADO);
    }
    @Test void traduceSoapFaultSinConfundirloConAceptacion() {
        response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault><faultstring>Error</faultstring></s:Fault></s:Body></s:Envelope>";
        assertThatThrownBy(() -> adapter.disponibilidad("A")).isInstanceOf(ProveedorNoDisponibleException.class).hasMessageContaining("SOAP Fault");
    }
}
