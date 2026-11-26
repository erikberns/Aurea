package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.time.Duration;

/**
 * Parámetros del gateway, todos por configuración (IA_HABILITADA, IA_TIMEOUT_MS, etc.).
 *
 * @param habilitada            feature flag: con false no se llama al modelo y se usa el fallback (plan de apagado)
 * @param timeout               tiempo máximo de una llamada al modelo
 * @param maxSolicitudesPorMinuto límite por usuario (OWASP LLM10: consumo sin límite)
 * @param fallosParaAbrir       fallas seguidas que abren el circuit breaker
 * @param esperaCircuito        cuánto queda abierto el circuito
 * @param costoEntradaUsdPorMillon precio de 1 M de tokens de entrada (0 en el plan gratuito)
 * @param costoSalidaUsdPorMillon  precio de 1 M de tokens de salida (0 en el plan gratuito)
 */
public record ConfiguracionIA(boolean habilitada, Duration timeout, int maxSolicitudesPorMinuto,
                              int fallosParaAbrir, Duration esperaCircuito,
                              double costoEntradaUsdPorMillon, double costoSalidaUsdPorMillon) {
}
