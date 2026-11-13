package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

public interface ShippingStrategy {
    boolean soporta(double cartTotal);
    double calculateShippingCost(double cartTotal);
}
