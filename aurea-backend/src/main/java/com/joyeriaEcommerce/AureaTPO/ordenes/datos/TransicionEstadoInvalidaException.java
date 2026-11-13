package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

/**
 * Excepción de dominio lanzada cuando se intenta una transición no permitida
 * en el ciclo de vida de una orden (Patrón State).
 * Extiende IllegalStateException para traducirse en HTTP 409 Conflict.
 */
public class TransicionEstadoInvalidaException extends IllegalStateException {

    private final OrderStatus estadoActual;
    private final String operacion;

    public TransicionEstadoInvalidaException(OrderStatus estadoActual, String operacion) {
        super("Transición inválida: no se puede ejecutar '" + operacion + "' cuando la orden está en estado " + estadoActual);
        this.estadoActual = estadoActual;
        this.operacion = operacion;
    }

    public OrderStatus getEstadoActual() {
        return estadoActual;
    }

    public String getOperacion() {
        return operacion;
    }
}

