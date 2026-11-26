package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter REST hacia Google Gemini (generateContent). Usa la API HTTP directamente, como el adapter
 * de Stripe, para que el contrato quede explícito. Pide la respuesta en JSON (responseMimeType) y
 * el negocio la valida igual, porque un modelo puede no respetar el formato.
 *
 * <p>La clave viaja solo en la cabecera x-goog-api-key y nunca se escribe en logs ni en errores.</p>
 */
public class GeminiModeloAdapter implements ProveedorModeloIA {

    private static final Logger log = LoggerFactory.getLogger(GeminiModeloAdapter.class);

    /** Tiempo máximo de cada intento: deja lugar a un reintento dentro del timeout del gateway. */
    static final Duration TIMEOUT_POR_INTENTO = Duration.ofSeconds(4);

    private final HttpClient client;
    private final String base;
    private final String claveApi;
    private final String modelo;
    private final Duration timeout;
    private final ObjectMapper json;
    private final String nivelRazonamiento;

    public GeminiModeloAdapter(String url, String claveApi, String modelo, Duration timeout, ObjectMapper json) {
        this(url, claveApi, modelo, timeout, json, "low");
    }

    /**
     * @param nivelRazonamiento thinkingLevel de Gemini 3 (minimal, low, medium, high). Por defecto los modelos
     *                          razonan en "high", que es lento para una descripción corta; vacío no lo envía.
     */
    public GeminiModeloAdapter(String url, String claveApi, String modelo, Duration timeout, ObjectMapper json,
                               String nivelRazonamiento) {
        if (claveApi == null || claveApi.isBlank()) {
            throw new IllegalStateException("Para usar Gemini hay que completar IA_API_KEY en el .env "
                    + "(se obtiene gratis en Google AI Studio).");
        }
        if (modelo == null || !modelo.matches("[A-Za-z0-9._-]{1,80}")) {
            throw new IllegalStateException("IA_MODELO no es un identificador de modelo válido");
        }
        this.base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.claveApi = claveApi.trim();
        this.modelo = modelo;
        this.timeout = timeout;
        this.json = json;
        this.nivelRazonamiento = nivelRazonamiento == null ? "" : nivelRazonamiento.trim().toLowerCase(java.util.Locale.ROOT);
        if (!this.nivelRazonamiento.matches("|minimal|low|medium|high")) {
            throw new IllegalStateException("IA_NIVEL_RAZONAMIENTO debe ser minimal, low, medium, high o vacío");
        }
        // HTTP/1.1: con HTTP/2, el cliente reutiliza una conexión que Google cierra cuando queda inactiva,
        // y la llamada siguiente se cuelga hasta el timeout (se vio en el eval: respuestas de ~1 s o cortes a 8 s).
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT_POR_INTENTO)
                .build();
    }

    @Override
    public String nombre() {
        return "gemini";
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public RespuestaModelo generar(SolicitudModelo solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("systemInstruction", Map.of("parts", List.of(Map.of("text", solicitud.instrucciones()))));
        cuerpo.put("contents", List.of(Map.of("role", "user",
                "parts", List.of(Map.of("text", solicitud.entrada())))));
        Map<String, Object> generacion = new LinkedHashMap<>();
        generacion.put("responseMimeType", "application/json");
        generacion.put("temperature", solicitud.temperatura());
        generacion.put("maxOutputTokens", solicitud.maxTokensSalida());
        if (!nivelRazonamiento.isEmpty()) {
            generacion.put("thinkingConfig", Map.of("thinkingLevel", nivelRazonamiento));
        }
        cuerpo.put("generationConfig", generacion);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/v1beta/models/" + modelo + ":generateContent"))
                .timeout(timeout.compareTo(TIMEOUT_POR_INTENTO) < 0 ? timeout : TIMEOUT_POR_INTENTO)
                .header("x-goog-api-key", claveApi)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo), StandardCharsets.UTF_8))
                .build();
        // Como máximo dos intentos: el segundo solo si el primero se colgó (sin respuesta en 4 s) o si Google
        // devolvió 503 ("high demand") o 429, que suelen ser fallas momentáneas. El gateway corta todo a los 8 s.
        HttpResponse<String> response = intentar(request, 1);
        if (response == null) {
            response = intentar(request, 2);
        } else if (response.statusCode() == 503 || response.statusCode() == 429) {
            esperar(300);
            HttpResponse<String> segundo = intentar(request, 2);
            if (segundo != null) response = segundo;
        }
        if (response == null) {
            throw new ModeloNoDisponibleException("Gemini no respondió a tiempo en 2 intentos", null);
        }

        Map<String, Object> datos = mapa(leer(response.body()));
        if (response.statusCode() != 200) {
            String mensaje = texto(mapa(datos.get("error")).get("message"));
            throw new ModeloNoDisponibleException("Gemini respondió " + response.statusCode()
                    + (mensaje.isBlank() ? "" : ": " + recortar(mensaje)), null);
        }
        StringBuilder texto = new StringBuilder();
        if (datos.get("candidates") instanceof List<?> candidatos && !candidatos.isEmpty()) {
            Map<String, Object> contenido = mapa(mapa(candidatos.get(0)).get("content"));
            if (contenido.get("parts") instanceof List<?> partes) {
                for (Object parte : partes) texto.append(texto(mapa(parte).get("text")));
            }
        }
        Map<String, Object> uso = mapa(datos.get("usageMetadata"));
        return new RespuestaModelo(texto.toString(), entero(uso.get("promptTokenCount")),
                entero(uso.get("candidatesTokenCount")));
    }

    /** Un intento. Devuelve null si no hubo respuesta a tiempo; registra el resultado sin datos sensibles. */
    private HttpResponse<String> intentar(HttpRequest request, int numero) {
        long inicio = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("Gemini intento {}: HTTP {} en {} ms", numero, response.statusCode(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
            return response;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("La consulta a Gemini fue interrumpida", ex);
        } catch (IOException ex) {
            log.warn("Gemini intento {}: sin respuesta en {} ms ({})", numero,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio), ex.getClass().getSimpleName());
            return null;
        }
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("La consulta a Gemini fue interrumpida", ex);
        }
    }

    private Object leer(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) return Map.of();
        try {
            return json.readValue(cuerpo, Map.class);
        } catch (RuntimeException ex) {
            throw new ModeloNoDisponibleException("Gemini devolvió una respuesta ilegible", ex);
        }
    }

    private static Map<String, Object> mapa(Object valor) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        if (valor instanceof Map<?, ?> m) m.forEach((k, v) -> resultado.put(String.valueOf(k), v));
        return resultado;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private static int entero(Object valor) {
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private static String recortar(String mensaje) {
        return mensaje.length() <= 200 ? mensaje : mensaje.substring(0, 200) + "…";
    }
}
