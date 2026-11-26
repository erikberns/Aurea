package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.util.Optional;
import java.util.function.Function;

/**
 * Única puerta de entrada a los modelos de lenguaje (Facade sobre el Adapter {@link ProveedorModeloIA}).
 * Aplica el feature flag, el límite por usuario, el circuit breaker y el timeout, y registra una traza
 * por llamada (versión del prompt, modelo, tokens, costo y latencia; nunca el texto del usuario).
 */
public interface IAGateway {

    /**
     * Llama al modelo y pasa su respuesta por {@code interpretar}, que debe validarla y devolver vacío
     * si no cumple el contrato. Nunca lanza por fallas del modelo: devuelve un resultado sin valor y
     * el motivo, para que la funcionalidad aplique su fallback.
     *
     * @throws LimiteIAExcedidoException si el usuario superó el límite de solicitudes
     */
    <T> ResultadoIA<T> completar(PedidoIA pedido, Function<String, Optional<T>> interpretar);

    EstadoGatewayIA estado();
}
