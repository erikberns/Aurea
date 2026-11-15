package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

/**
 * Puerto hacia la pasarela de pago externa (Adapter). El negocio no conoce el protocolo
 * ni el proveedor: cambiar Stripe por otra pasarela solo requiere otra implementación.
 */
public interface IPasarelaPago {
    String nombre();
    ResultadoPasarelaDTO autorizar(String claveIdempotencia, long montoCentavos, String moneda,
                                   String metodoPago, Long ordenId);
    void capturar(String referencia, String claveIdempotencia);
    void cancelar(String referencia, String claveIdempotencia);
}
