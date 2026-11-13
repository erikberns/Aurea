package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios puros del Patrón State en la entidad central Order.
 * Verifica transiciones válidas e inválidas (Clase 12, slide 11).
 */
class OrderStateTests {

    private static Order nuevaOrden(OrderStatus estadoInicial) {
        return new Order("Av. Santa Fe 1234", LocalDate.now(), estadoInicial, 50000.0, 1L);
    }

    @Test
    void permiteTransicionesValidasDelCicloDeVida() {
        Order order = nuevaOrden(OrderStatus.PENDING);

        order.confirmar();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        order.completar();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        order.cancelar();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rechazaCompletarUnaOrdenPendiente() {
        Order order = nuevaOrden(OrderStatus.PENDING);

        assertThatThrownBy(order::completar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("completar")
                .hasMessageContaining("PENDING");
    }

    @Test
    void rechazaConfirmarUnaOrdenYaConfirmada() {
        Order order = nuevaOrden(OrderStatus.CONFIRMED);

        assertThatThrownBy(order::confirmar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("confirmar")
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void rechazaConfirmarOCompletarUnaOrdenCompletada() {
        Order order = nuevaOrden(OrderStatus.COMPLETED);

        assertThatThrownBy(order::confirmar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("confirmar");

        assertThatThrownBy(order::completar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("completar");
    }

    @Test
    void estadoCanceladoEsFinalYRechazaCualquierTransicion() {
        Order order = nuevaOrden(OrderStatus.CANCELLED);

        assertThatThrownBy(order::confirmar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CANCELLED");

        assertThatThrownBy(order::completar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CANCELLED");

        assertThatThrownBy(order::cancelar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CANCELLED");
    }
}

