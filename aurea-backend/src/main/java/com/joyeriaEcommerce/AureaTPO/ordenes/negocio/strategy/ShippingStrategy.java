package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

public interface ShippingStrategy {
    double calculateShippingCost(double cartTotal);
}
