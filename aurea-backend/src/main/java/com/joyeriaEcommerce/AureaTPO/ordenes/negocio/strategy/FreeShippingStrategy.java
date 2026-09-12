package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import org.springframework.stereotype.Component;

@Component
public class FreeShippingStrategy implements ShippingStrategy {
    @Override
    public double calculateShippingCost(double cartTotal) {
        return 0.0;
    }
}
