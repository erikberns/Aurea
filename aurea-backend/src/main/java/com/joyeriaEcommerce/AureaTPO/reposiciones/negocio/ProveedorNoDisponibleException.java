package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
public class ProveedorNoDisponibleException extends RuntimeException {
    /** Segundos sugeridos antes de reintentar (Retry-After); 0 si no hay una espera conocida. */
    private final long segundosParaReintentar;

    public ProveedorNoDisponibleException(String mensaje, Throwable causa) { this(mensaje, causa, 0); }

    public ProveedorNoDisponibleException(String mensaje, Throwable causa, long segundosParaReintentar) {
        super(mensaje, causa);
        this.segundosParaReintentar = segundosParaReintentar;
    }

    public long getSegundosParaReintentar() { return segundosParaReintentar; }
}
