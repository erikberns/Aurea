package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import java.util.Arrays;

/**
 * Métodos de pago de prueba de Stripe. El cliente elige un código de esta lista blanca;
 * el backend nunca recibe ni almacena números de tarjeta. En producción, el token
 * (PaymentMethod) lo generaría Stripe Elements en el navegador.
 */
public enum MetodoPagoPrueba {
    VISA_APROBADA("pm_card_visa", "Visa de prueba · aprobada", true),
    MASTERCARD_APROBADA("pm_card_mastercard", "Mastercard de prueba · aprobada", true),
    RECHAZADA("pm_card_chargeDeclined", "Tarjeta rechazada por el emisor", false),
    FONDOS_INSUFICIENTES("pm_card_chargeDeclinedInsufficientFunds", "Tarjeta sin fondos suficientes", false);

    private final String token;
    private final String descripcion;
    private final boolean aprobacionEsperada;

    MetodoPagoPrueba(String token, String descripcion, boolean aprobacionEsperada) {
        this.token = token; this.descripcion = descripcion; this.aprobacionEsperada = aprobacionEsperada;
    }

    public String token() { return token; }
    public String descripcion() { return descripcion; }
    public boolean aprobacionEsperada() { return aprobacionEsperada; }

    public static MetodoPagoPrueba desde(String codigo) {
        return Arrays.stream(values()).filter(m -> m.name().equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no admitido"));
    }
}
