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
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter REST hacia Google Gemini (generateContent). Usa la API HTTP directamente, como el adapter
 * de Stripe, para que el contrato quede explícito. Pide la respuesta en JSON (responseMimeType) y
 * el negocio la valida igual, porque un modelo puede no respetar el formato.
 *
 * <p>La clave viaja solo en la cabecera x-goog-api-key y nunca se escribe en logs ni en errores.</p>
 */
public class GeminiModeloAdapter implements ProveedorModeloIA {

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
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
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
                .timeout(timeout)
                .header("x-goog-api-key", claveApi)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = enviar(request);
        // Un 503 ("high demand") o un 429 suelen ser picos momentáneos: se reintenta una sola vez.
        // El gateway igual corta la llamada completa a los 5 s.
        if (response.statusCode() == 503 || response.statusCode() == 429) {
            esperar(400);
            response = enviar(request);
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

    private HttpResponse<String> enviar(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("La consulta a Gemini fue interrumpida", ex);
        } catch (IOException ex) {
            throw new ModeloNoDisponibleException("Gemini no respondió a tiempo", ex);
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
