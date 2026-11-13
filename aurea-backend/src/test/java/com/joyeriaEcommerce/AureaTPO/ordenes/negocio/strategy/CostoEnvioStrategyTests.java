package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios puros del Patrón Strategy para cálculo de costo de envío.
 */
class CostoEnvioStrategyTests {

    private final CostoEnvio costoEnvio = new CostoEnvio(
            new FreeShippingStrategy(),
            new StandardShippingStrategy());

    @Test
    void aplicaEnvioEstandarCuandoElSubtotalEsMenorAlUmbral() {
        assertThat(costoEnvio.calcular(45000.0)).isEqualTo(4500.0);
        assertThat(costoEnvio.calcular(59999.99)).isEqualTo(4500.0);
    }

    @Test
    void aplicaEnvioGratisCuandoElSubtotalAlcanzaOSuperaElUmbral() {
        assertThat(costoEnvio.calcular(60000.0)).isEqualTo(0.0);
        assertThat(costoEnvio.calcular(120000.0)).isEqualTo(0.0);
    }
}

