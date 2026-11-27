package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Validaciones de la fachada de compra antes de crear el pedido. */
@SpringBootTest
class CheckoutFacadeReglasTests {
    @Autowired ICheckout checkout;
    @Autowired IProductos productos;
    @Value("${app.admin.email}") String usuario;

    private long producto(int stock) {
        return productos.createProduct("Checkout regla", "Plata", 1500.0, stock, null, "").id();
    }

    @Test
    void direccionYCarritoSonObligatorios() {
        long id = producto(2);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, null, Map.of(id, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "  ", Map.of(id, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadesInvalidasSeRechazan() {
        long id = producto(2);
        Map<Long, Integer> claveNula = new HashMap<>();
        claveNula.put(null, 1);
        Map<Long, Integer> cantidadNula = new HashMap<>();
        cantidadNula.put(id, null);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", claveNula)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", cantidadNula)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", Map.of(id, 0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productoInactivoOSinStockNoGeneraPedido() {
        long sinStock = producto(1);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", Map.of(sinStock, 2))).isInstanceOf(IllegalStateException.class);
        long inactivo = producto(5);
        productos.cambiarEstado(inactivo, false);
        assertThatThrownBy(() -> checkout.placeOrder(usuario, "Calle 1", Map.of(inactivo, 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pedidoValidoQuedaPendienteConElTotalCalculado() {
        long id = producto(3);
        OrderDTO pedido = checkout.placeOrder(usuario, "Av. Corrientes 1234", Map.of(id, 2));
        assertThat(pedido.status()).isEqualTo("PENDING");
        assertThat(pedido.total()).isGreaterThanOrEqualTo(3000.0);
    }
}
