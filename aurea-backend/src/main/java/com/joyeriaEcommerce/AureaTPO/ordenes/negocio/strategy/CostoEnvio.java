package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICalculoEnvio;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Selecciona polimórficamente una estrategia de envío (OCP + Strategy); no conserva estado conversacional. */
@Service
public class CostoEnvio implements ICalculoEnvio {
    private final List<ShippingStrategy> estrategias;

    public CostoEnvio(@Qualifier("freeShippingStrategy") ShippingStrategy gratis,
                      @Qualifier("standardShippingStrategy") ShippingStrategy estandar) {
        this.estrategias = List.of(gratis, estandar);
    }

    @Override
    public double calcular(double subtotal) {
        ShippingStrategy estrategia = estrategias.stream()
                .filter(s -> s.soporta(subtotal))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay estrategia de envío para el subtotal: " + subtotal));
        return estrategia.calculateShippingCost(subtotal);
    }
}
