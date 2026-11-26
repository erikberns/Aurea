package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Elige el proveedor del modelo según IA_PROVEEDOR:
 * <ul>
 *   <li>{@code auto} (por defecto): Gemini si hay IA_API_KEY; si no, el modelo simulado.</li>
 *   <li>{@code simulado}: siempre el simulado, sin red (CI y pruebas).</li>
 *   <li>{@code gemini}: exige IA_API_KEY; sin ella el backend no arranca y lo explica.</li>
 * </ul>
 */
@Configuration
public class IAConfig {

    private static final Logger log = LoggerFactory.getLogger(IAConfig.class);

    @Bean
    ConfiguracionIA configuracionIA(
            @Value("${app.ia.habilitada:true}") boolean habilitada,
            @Value("${app.ia.timeout-ms:8000}") long timeoutMs,
            @Value("${app.ia.max-solicitudes-por-minuto:10}") int maxPorMinuto,
            @Value("${app.ia.costo-entrada-usd-por-millon:0}") double costoEntrada,
            @Value("${app.ia.costo-salida-usd-por-millon:0}") double costoSalida) {
        return new ConfiguracionIA(habilitada, Duration.ofMillis(timeoutMs), maxPorMinuto, 3,
                Duration.ofSeconds(30), costoEntrada, costoSalida);
    }

    @Bean
    ProveedorModeloIA proveedorModeloIA(
            @Value("${app.ia.proveedor:auto}") String proveedor,
            @Value("${app.ia.api-key:}") String claveApi,
            @Value("${app.ia.modelo:gemini-3.8-flash}") String modelo,
            @Value("${app.ia.url:https://generativelanguage.googleapis.com}") String url,
            @Value("${app.ia.timeout-ms:8000}") long timeoutMs,
            @Value("${app.ia.nivel-razonamiento:low}") String nivelRazonamiento,
            ObjectMapper json) {
        String elegido = proveedor == null ? "auto" : proveedor.trim().toLowerCase(Locale.ROOT);
        if (elegido.equals("auto")) {
            elegido = claveApi == null || claveApi.isBlank() ? "simulado" : "gemini";
        }
        ProveedorModeloIA resultado = switch (elegido) {
            case "simulado" -> new ModeloSimuladoAdapter(json);
            case "gemini" -> new GeminiModeloAdapter(url, claveApi, modelo, Duration.ofMillis(timeoutMs), json,
                    nivelRazonamiento);
            default -> throw new IllegalStateException("IA_PROVEEDOR debe ser auto, simulado o gemini");
        };
        log.info("Asistente de IA: proveedor {} con el modelo {}", resultado.nombre(), resultado.modelo());
        return resultado;
    }
}
