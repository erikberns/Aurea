package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

/** La pasarela no respondió o respondió algo inesperado (HTTP 503). */
public class PasarelaNoDisponibleException extends RuntimeException {
    /** Segundos sugeridos antes de reintentar (Retry-After); 0 si no hay una espera conocida. */
    private final long segundosParaReintentar;

    public PasarelaNoDisponibleException(String mensaje, Throwable causa) { this(mensaje, causa, 0); }

    public PasarelaNoDisponibleException(String mensaje, Throwable causa, long segundosParaReintentar) {
        super(mensaje, causa);
        this.segundosParaReintentar = segundosParaReintentar;
    }

    public long getSegundosParaReintentar() { return segundosParaReintentar; }
}
