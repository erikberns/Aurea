package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import org.springframework.stereotype.Component;

@Component
public class StandardShippingStrategy implements ShippingStrategy {
    @Override
    public double calculateShippingCost(double cartTotal) {
        return 4500.0;
    }
}
