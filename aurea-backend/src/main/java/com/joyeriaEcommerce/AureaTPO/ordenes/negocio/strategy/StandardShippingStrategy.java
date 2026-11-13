package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import org.springframework.stereotype.Component;

@Component
public class StandardShippingStrategy implements ShippingStrategy {
    private static final double UMBRAL_ENVIO_GRATIS = 60000.0;

    @Override
    public boolean soporta(double cartTotal) {
        return cartTotal < UMBRAL_ENVIO_GRATIS;
    }

    @Override
    public double calculateShippingCost(double cartTotal) {
        return 4500.0;
    }
}
