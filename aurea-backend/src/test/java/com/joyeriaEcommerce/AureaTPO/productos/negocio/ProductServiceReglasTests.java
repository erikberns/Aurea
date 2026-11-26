package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.errores.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de negocio del catálogo: cada validación del servicio de productos (camino feliz y ramas de error). */
@SpringBootTest
class ProductServiceReglasTests {
    private static final long INEXISTENTE = 987654321L;
    @Autowired IProductos productos;

    private ProductDTO nuevo(int stock) {
        return productos.createProduct("Regla " + UUID.randomUUID(), "Plata 925", 1000.0, stock, null, "img.jpg");
    }

    private long categoria() {
        return productos.getCategories().getFirst().id();
    }

    @Test
    void crearValidaNombrePrecioStockYCategoria() {
        assertThatThrownBy(() -> productos.createProduct(null, "", 1.0, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("  ", "", 1.0, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", null, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", Double.NaN, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", Double.POSITIVE_INFINITY, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", -1.0, 1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", 1.0, null, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", 1.0, -1, null, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.createProduct("A", "", 1.0, 1, INEXISTENTE, "")).isInstanceOf(IllegalArgumentException.class);

        ProductDTO conCategoria = productos.createProduct("Con categoría", "Oro", 10.0, 0, categoria(), "");
        assertThat(conCategoria.category()).isNotNull();
        assertThat(conCategoria.active()).isTrue();
        assertThat(nuevo(0).category()).isNull();
    }

    @Test
    void operacionesSobreProductoInexistenteDevuelvenRecursoNoEncontrado() {
        assertThatThrownBy(() -> productos.getProductById(INEXISTENTE)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.updatePrice(INEXISTENTE, 5.0)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.updateStock(INEXISTENTE, 5)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.updateProduct(INEXISTENTE, "x", "x", "x", null)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.cambiarEstado(INEXISTENTE, false)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.deleteProduct(INEXISTENTE)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void preciosYStockInvalidosSeRechazanAntesDeBuscar() {
        long id = nuevo(3).id();
        assertThatThrownBy(() -> productos.updatePrice(id, -5.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.updateStock(id, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(productos.updatePrice(id, 2500.0).price()).isEqualTo(2500.0);
        assertThat(productos.updateStock(id, 9).stock()).isEqualTo(9);
    }

    @Test
    void editarConservaLosCamposVaciosYValidaLaCategoria() {
        ProductDTO original = nuevo(1);
        ProductDTO sinCambios = productos.updateProduct(original.id(), " ", null, "", null);
        assertThat(sinCambios.name()).isEqualTo(original.name());
        assertThat(sinCambios.description()).isEqualTo(original.description());
        assertThat(sinCambios.imageUrl()).isEqualTo(original.imageUrl());

        ProductDTO editado = productos.updateProduct(original.id(), "Nuevo nombre", "Nueva descripción", "nueva.jpg", categoria());
        assertThat(editado.name()).isEqualTo("Nuevo nombre");
        assertThat(editado.description()).isEqualTo("Nueva descripción");
        assertThat(editado.imageUrl()).isEqualTo("nueva.jpg");
        assertThat(editado.category()).isNotNull();

        assertThatThrownBy(() -> productos.updateProduct(original.id(), null, null, null, INEXISTENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cambiarEstadoYEliminar() {
        long id = nuevo(1).id();
        assertThat(productos.cambiarEstado(id, false).active()).isFalse();
        productos.deleteProduct(id);
        assertThatThrownBy(() -> productos.getProductById(id)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void descontarStockValidaEntradaEstadoYCantidad() {
        assertThatThrownBy(() -> productos.descontarStock(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.descontarStock(Map.of())).isInstanceOf(IllegalArgumentException.class);
        Map<Long, Integer> claveNula = new HashMap<>();
        claveNula.put(null, 1);
        assertThatThrownBy(() -> productos.descontarStock(claveNula)).isInstanceOf(IllegalArgumentException.class);
        long id = nuevo(2).id();
        Map<Long, Integer> cantidadNula = new HashMap<>();
        cantidadNula.put(id, null);
        assertThatThrownBy(() -> productos.descontarStock(cantidadNula)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.descontarStock(Map.of(id, 0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.descontarStock(Map.of(INEXISTENTE, 1))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> productos.descontarStock(Map.of(id, 3))).isInstanceOf(IllegalStateException.class);

        productos.descontarStock(Map.of(id, 2));
        assertThat(productos.getProductById(id).stock()).isZero();

        long inactivo = nuevo(5).id();
        productos.cambiarEstado(inactivo, false);
        assertThatThrownBy(() -> productos.descontarStock(Map.of(inactivo, 1))).isInstanceOf(IllegalStateException.class);
        assertThat(productos.getProductById(inactivo).stock()).isEqualTo(5);
    }

    @Test
    void aplicarReposicionValidaYEsIdempotente() {
        long id = nuevo(1).id();
        String evento = UUID.randomUUID().toString();
        assertThatThrownBy(() -> productos.aplicarReposicion("no-es-uuid", id, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.aplicarReposicion(evento, id, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.aplicarReposicion(evento, id, 10001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productos.aplicarReposicion(evento, INEXISTENTE, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(productos.reposicionAplicada(evento)).isFalse();

        productos.aplicarReposicion(evento, id, 4);
        productos.aplicarReposicion(evento, id, 4);
        assertThat(productos.getProductById(id).stock()).isEqualTo(5);
        assertThat(productos.reposicionAplicada(evento)).isTrue();

        long otro = nuevo(1).id();
        assertThatThrownBy(() -> productos.aplicarReposicion(evento, otro, 4)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> productos.aplicarReposicion(evento, id, 7)).isInstanceOf(IllegalStateException.class);
        assertThat(productos.getProductById(id).stock()).isEqualTo(5);
    }
}
