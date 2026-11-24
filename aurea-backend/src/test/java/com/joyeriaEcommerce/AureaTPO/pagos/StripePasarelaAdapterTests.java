package com.joyeriaEcommerce.AureaTPO.pagos;

import com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia.CircuitBreaker;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PasarelaNoDisponibleException;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.ResultadoPasarelaDTO;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.StripePasarelaAdapter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica el contrato REST con Stripe contra un servidor HTTP local que imita sus respuestas. */
class StripePasarelaAdapterTests {
    record Solicitud(String ruta, String autorizacion, String idempotencia, String cuerpo) {}

    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();
    private HttpServer servidor;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/payment_intents", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String ruta = intercambio.getRequestURI().getPath();
            solicitudes.add(new Solicitud(ruta, intercambio.getRequestHeaders().getFirst("Authorization"),
                    intercambio.getRequestHeaders().getFirst("Idempotency-Key"), cuerpo));
            int status = 200;
            String json;
            if (ruta.endsWith("/capture")) {
                json = "{\"id\":\"pi_123\",\"status\":\"succeeded\"}";
            } else if (ruta.endsWith("/cancel")) {
                json = "{\"id\":\"pi_123\",\"status\":\"canceled\"}";
            } else if (cuerpo.contains("pm_card_chargeDeclinedInsufficientFunds")) {
                status = 402;
                json = "{\"error\":{\"type\":\"card_error\",\"code\":\"card_declined\","
                        + "\"decline_code\":\"insufficient_funds\",\"message\":\"Your card has insufficient funds.\","
                        + "\"payment_intent\":{\"id\":\"pi_rechazado\",\"status\":\"requires_payment_method\"}}}";
            } else if (cuerpo.contains("pm_card_pasarela_caida")) {
                status = 503;
                json = "{\"error\":{\"type\":\"api_error\",\"message\":\"Service unavailable\"}}";
            } else if (cuerpo.contains("currency=xxx")) {
                status = 400;
                json = "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Invalid currency: xxx\"}}";
            } else {
                json = "{\"id\":\"pi_123\",\"object\":\"payment_intent\",\"status\":\"requires_capture\"}";
            }
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(status, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private StripePasarelaAdapter adapter() {
        return new StripePasarelaAdapter(url(), "sk_test_clave", 2000, JsonMapper.builder().build());
    }

    @Test
    void autorizaConRetencionManualAutenticacionBearerEIdempotencia() {
        ResultadoPasarelaDTO resultado = adapter().autorizar("aurea-orden-7-autorizacion", 1_450_000, "ars",
                "pm_card_visa", 7L);

        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.referencia()).isEqualTo("pi_123");
        Solicitud solicitud = solicitudes.get(0);
        assertThat(solicitud.ruta()).isEqualTo("/v1/payment_intents");
        assertThat(solicitud.autorizacion()).isEqualTo("Bearer sk_test_clave");
        assertThat(solicitud.idempotencia()).isEqualTo("aurea-orden-7-autorizacion");
        assertThat(solicitud.cuerpo()).contains("amount=1450000", "currency=ars", "payment_method=pm_card_visa",
                "capture_method=manual", "confirm=true", "metadata%5Borden_id%5D=7");
        assertThat(solicitud.cuerpo()).contains("allowed_payment_method_types%5B%5D=card");
        assertThat(solicitud.cuerpo().split("&"))
                .noneMatch(parametro -> parametro.startsWith("payment_method_types"));
    }

    @Test
    void elRechazoDeTarjetaSeInformaComoResultadoNoAprobado() {
        ResultadoPasarelaDTO resultado = adapter().autorizar("clave-rechazo", 1000, "ars",
                "pm_card_chargeDeclinedInsufficientFunds", 8L);

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.referencia()).isEqualTo("pi_rechazado");
        assertThat(resultado.detalle()).isEqualTo("Fondos insuficientes en la tarjeta.");
    }

    @Test
    void unErrorDeSolicitudIndicaPasarelaNoDisponible() {
        assertThatThrownBy(() -> adapter().autorizar("clave-error", 1000, "xxx", "pm_card_visa", 9L))
                .isInstanceOf(PasarelaNoDisponibleException.class)
                .hasMessageContaining("Invalid currency");
    }

    @Test
    void capturaYAnulacionUsanSusRecursos() {
        adapter().capturar("pi_123", "clave-captura");
        adapter().cancelar("pi_123", "clave-anulacion");

        assertThat(solicitudes).extracting(Solicitud::ruta)
                .containsExactly("/v1/payment_intents/pi_123/capture", "/v1/payment_intents/pi_123/cancel");
        assertThat(solicitudes).extracting(Solicitud::idempotencia)
                .containsExactly("clave-captura", "clave-anulacion");
    }

    @Test
    void unaPasarelaCaidaIndicaPasarelaNoDisponible() throws IOException {
        int puertoCerrado;
        try (var socket = new java.net.ServerSocket(0)) {
            puertoCerrado = socket.getLocalPort();
        }
        var adapter = new StripePasarelaAdapter("http://127.0.0.1:" + puertoCerrado, "sk_test_clave", 2000,
                JsonMapper.builder().build());
        assertThatThrownBy(() -> adapter.autorizar("clave-caida", 1000, "ars", "pm_card_visa", 10L))
                .isInstanceOf(PasarelaNoDisponibleException.class);
    }

    @Test
    void trasTresErroresSeguidosElCircuitoSeAbreYNoVuelveALlamarAStripe() {
        var adapter = adapter();
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> adapter.autorizar("clave-503", 1000, "ars", "pm_card_pasarela_caida", 11L))
                    .isInstanceOf(PasarelaNoDisponibleException.class);
        }
        assertThat(adapter.estadoCircuito()).isEqualTo(CircuitBreaker.Estado.ABIERTO);
        assertThatThrownBy(() -> adapter.autorizar("clave-siguiente", 1000, "ars", "pm_card_visa", 12L))
                .isInstanceOfSatisfying(PasarelaNoDisponibleException.class, ex -> {
                    assertThat(ex.getSegundosParaReintentar()).isPositive();
                    assertThat(ex.getMessage()).contains("carrito se conservó");
                });
        assertThat(solicitudes).as("con el circuito abierto no se envía la cuarta solicitud").hasSize(3);
    }

    @Test
    void losRechazosDeTarjetaNoAbrenElCircuito() {
        var adapter = adapter();
        for (int i = 0; i < 5; i++) {
            ResultadoPasarelaDTO resultado = adapter.autorizar("clave-rechazo-" + i, 1000, "ars",
                    "pm_card_chargeDeclinedInsufficientFunds", 13L);
            assertThat(resultado.aprobado()).isFalse();
        }
        assertThat(adapter.estadoCircuito()).isEqualTo(CircuitBreaker.Estado.CERRADO);
    }

    @Test
    void rechazaClavesProductivas() {
        assertThatThrownBy(() -> new StripePasarelaAdapter(url(), "sk_live_clave", 2000, JsonMapper.builder().build()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new StripePasarelaAdapter(url(), "", 2000, JsonMapper.builder().build()))
                .isInstanceOf(IllegalStateException.class);
    }
}
