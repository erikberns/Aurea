package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

/** La pasarela no respondió o respondió algo inesperado (HTTP 503). */
public class PasarelaNoDisponibleException extends RuntimeException {
    public PasarelaNoDisponibleException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
