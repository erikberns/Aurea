package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

/** La pasarela rechazó el pago: la compra completa se revierte (HTTP 402). */
public class PagoRechazadoException extends RuntimeException {
    public PagoRechazadoException(String mensaje) { super(mensaje); }
}
