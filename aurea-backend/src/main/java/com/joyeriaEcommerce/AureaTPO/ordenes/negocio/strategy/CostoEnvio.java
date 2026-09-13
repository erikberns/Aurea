package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICalculoEnvio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Selecciona una estrategia de envío; no conserva estado conversacional. */
@Service
public class CostoEnvio implements ICalculoEnvio {
    private final ShippingStrategy gratis;
    private final ShippingStrategy estandar;

    public CostoEnvio(@Qualifier("freeShippingStrategy") ShippingStrategy gratis,
                      @Qualifier("standardShippingStrategy") ShippingStrategy estandar) {
        this.gratis = gratis;
        this.estandar = estandar;
    }

    @Override
    public double calcular(double subtotal) {
        ShippingStrategy estrategia = subtotal >= 60000 ? gratis : estandar;
        return estrategia.calculateShippingCost(subtotal);
    }
}
