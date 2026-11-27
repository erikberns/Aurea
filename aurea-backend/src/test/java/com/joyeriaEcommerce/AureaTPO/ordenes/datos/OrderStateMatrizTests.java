package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Matriz completa del Patrón State: 4 estados x 3 acciones = 12 combinaciones.
 * 5 transiciones válidas y 7 inválidas; cada inválida lanza TransicionEstadoInvalidaException (409 en la API)
 * y deja el pedido en el mismo estado.
 */
class OrderStateMatrizTests {

    private static Order orden(OrderStatus estado) {
        return new Order("Av. Santa Fe 1234", LocalDate.now(), estado, 50000.0, 1L);
    }

    private static Consumer<Order> accion(String nombre) {
        return switch (nombre) {
            case "confirmar" -> Order::confirmar;
            case "completar" -> Order::completar;
            case "cancelar" -> Order::cancelar;
            default -> throw new IllegalArgumentException(nombre);
        };
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @CsvSource({
            "PENDING,   confirmar, CONFIRMED",
            "PENDING,   cancelar,  CANCELLED",
            "CONFIRMED, completar, COMPLETED",
            "CONFIRMED, cancelar,  CANCELLED",
            "COMPLETED, cancelar,  CANCELLED"
    })
    void transicionesValidas(OrderStatus inicial, String nombreAccion, OrderStatus esperado) {
        Order order = orden(inicial);
        accion(nombreAccion).accept(order);
        assertThat(order.getStatus()).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} no permite {1}")
    @CsvSource({
            "PENDING,   completar",
            "CONFIRMED, confirmar",
            "COMPLETED, confirmar",
            "COMPLETED, completar",
            "CANCELLED, confirmar",
            "CANCELLED, completar",
            "CANCELLED, cancelar"
    })
    void transicionesInvalidasLanzanExcepcionYNoCambianElEstado(OrderStatus inicial, String nombreAccion) {
        Order order = orden(inicial);
        assertThatThrownBy(() -> accion(nombreAccion).accept(order))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(nombreAccion)
                .hasMessageContaining(inicial.name());
        assertThat(order.getStatus()).isEqualTo(inicial);
    }
}
