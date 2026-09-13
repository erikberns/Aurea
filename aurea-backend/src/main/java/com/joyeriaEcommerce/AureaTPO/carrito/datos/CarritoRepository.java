package com.joyeriaEcommerce.AureaTPO.carrito.datos;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * Almacenamiento temporal de identificadores y cantidades.
 * Spring inyecta una instancia nueva en cada ServicioDeCarrito de sesión.
 * El servicio sincroniza las operaciones y controla su ciclo de vida.
 */
@Repository
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CarritoRepository implements ICarritoRepository {
    private final Map<Long, Integer> cantidades = new LinkedHashMap<>();

    public Map<Long, Integer> consultar() {
        return new LinkedHashMap<>(cantidades);
    }

    public int cantidadDe(Long productoId) {
        return cantidades.getOrDefault(productoId, 0);
    }

    public void guardar(Long productoId, int cantidad) {
        cantidades.put(productoId, cantidad);
    }

    public void quitar(Long productoId) {
        cantidades.remove(productoId);
    }

    public void vaciar() {
        cantidades.clear();
    }
}
