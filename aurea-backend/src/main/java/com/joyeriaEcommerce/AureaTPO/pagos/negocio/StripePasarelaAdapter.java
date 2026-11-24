package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter REST hacia Stripe (partner moderno), en modo de prueba.
 * Usa la API HTTP directamente (no el SDK) para que el contrato REST quede explícito:
 * POST /v1/payment_intents (form-urlencoded), autenticación Bearer, cabecera Idempotency-Key
 * y códigos HTTP (200 aprobado, 402 tarjeta rechazada).
 */
@Component
@ConditionalOnProperty(name = "app.pagos.proveedor", havingValue = "stripe")
public class StripePasarelaAdapter implements IPasarelaPago {
    private final HttpClient client;
    private final String base;
    private final String claveSecreta;
    private final Duration timeout;
    private final ObjectMapper json;

    public StripePasarelaAdapter(@Value("${app.pagos.stripe.url:https://api.stripe.com}") String url,
                                 @Value("${app.pagos.stripe.secret-key:}") String claveSecreta,
                                 @Value("${app.pagos.timeout-ms:5000}") long timeoutMs,
                                 ObjectMapper json) {
        if (claveSecreta == null
                || !(claveSecreta.startsWith("sk_test_") || claveSecreta.startsWith("rk_test_"))) {
            throw new IllegalStateException("PAGOS_PROVEEDOR=stripe requiere STRIPE_SECRET_KEY con una clave "
                    + "de prueba (sk_test_...). Las claves productivas están bloqueadas en este proyecto.");
        }
        this.base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.claveSecreta = claveSecreta;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.json = json;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String nombre() {
        return "stripe";
    }

    @Override
    public ResultadoPasarelaDTO autorizar(String claveIdempotencia, long montoCentavos, String moneda,
                                          String metodoPago, Long ordenId) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("amount", String.valueOf(montoCentavos));
        form.put("currency", moneda);
        form.put("payment_method", metodoPago);
        form.put("allowed_payment_method_types[]", "card");
        form.put("capture_method", "manual"); // Solo retiene: se cobra después del commit.
        form.put("confirm", "true");
        form.put("description", "Áurea Joyas - orden " + ordenId);
        form.put("metadata[orden_id]", String.valueOf(ordenId));
        Respuesta respuesta = enviar("/v1/payment_intents", form, claveIdempotencia);

        if (respuesta.status() == 200) {
            String id = texto(respuesta.cuerpo(), "id");
            String estado = texto(respuesta.cuerpo(), "status");
            if ("requires_capture".equals(estado)) {
                return new ResultadoPasarelaDTO(true, id, estado, null);
            }
            // 3-D Secure u otros pasos interactivos no forman parte de esta demostración.
            if (id != null) cancelarSinPropagar(id, claveIdempotencia + "-cancelacion");
            return new ResultadoPasarelaDTO(false, id, estado,
                    "La tarjeta requiere un paso adicional no soportado (" + estado + ")");
        }
        Map<String, Object> error = mapa(respuesta.cuerpo().get("error"));
        if (respuesta.status() == 402 && "card_error".equals(texto(error, "type"))) {
            String referencia = texto(mapa(error.get("payment_intent")), "id");
            return new ResultadoPasarelaDTO(false, referencia, texto(error, "code"),
                    mensajeRechazo(texto(error, "code"), texto(error, "decline_code")));
        }
        throw new PasarelaNoDisponibleException(
                "La pasarela de pago respondió " + respuesta.status() + ": " + texto(error, "message"), null);
    }

    @Override
    public void capturar(String referencia, String claveIdempotencia) {
        Respuesta r = enviar("/v1/payment_intents/" + validarReferencia(referencia) + "/capture", Map.of(),
                claveIdempotencia);
        if (r.status() != 200) {
            throw new PasarelaNoDisponibleException("Stripe no capturó el pago: "
                    + texto(mapa(r.cuerpo().get("error")), "message"), null);
        }
    }

    @Override
    public void cancelar(String referencia, String claveIdempotencia) {
        Respuesta r = enviar("/v1/payment_intents/" + validarReferencia(referencia) + "/cancel", Map.of(),
                claveIdempotencia);
        if (r.status() != 200) {
            throw new PasarelaNoDisponibleException("Stripe no anuló la retención: "
                    + texto(mapa(r.cuerpo().get("error")), "message"), null);
        }
    }

    private void cancelarSinPropagar(String referencia, String claveIdempotencia) {
        try {
            cancelar(referencia, claveIdempotencia);
        } catch (RuntimeException ignorada) {
            // Stripe vence automáticamente las retenciones no capturadas.
        }
    }

    private Respuesta enviar(String ruta, Map<String, String> form, String claveIdempotencia) {
        String cuerpo = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(timeout)
                .header("Authorization", "Bearer " + claveSecreta)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Idempotency-Key", claveIdempotencia)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String texto = response.body();
            Map<String, Object> datos = texto == null || texto.isBlank()
                    ? Map.of() : mapa(json.readValue(texto, Map.class));
            return new Respuesta(response.statusCode(), datos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PasarelaNoDisponibleException("La consulta a la pasarela de pago fue interrumpida", ex);
        } catch (IOException ex) {
            throw new PasarelaNoDisponibleException(
                    "La pasarela de pago no respondió a tiempo. La compra no se registró; puede reintentar.", ex);
        } catch (RuntimeException ex) {
            throw new PasarelaNoDisponibleException("La pasarela de pago devolvió una respuesta ilegible", ex);
        }
    }

    private static String validarReferencia(String referencia) {
        if (referencia == null || !referencia.matches("[A-Za-z0-9_]{1,255}")) {
            throw new IllegalArgumentException("Referencia de pago inválida");
        }
        return referencia;
    }

    private static String mensajeRechazo(String codigo, String motivo) {
        if ("insufficient_funds".equals(motivo)) return "Fondos insuficientes en la tarjeta.";
        if ("expired_card".equals(codigo)) return "La tarjeta está vencida.";
        if ("incorrect_cvc".equals(codigo)) return "El código de seguridad es incorrecto.";
        if ("lost_card".equals(motivo) || "stolen_card".equals(motivo)) return "La tarjeta fue denunciada.";
        return "La tarjeta fue rechazada por el emisor.";
    }

    private static Map<String, Object> mapa(Object valor) {
        Map<String, Object> resultado = new HashMap<>();
        if (valor instanceof Map<?, ?> m) m.forEach((k, v) -> resultado.put(String.valueOf(k), v));
        return resultado;
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        return valor == null ? null : valor.toString();
    }

    private record Respuesta(int status, Map<String, Object> cuerpo) {}
}
