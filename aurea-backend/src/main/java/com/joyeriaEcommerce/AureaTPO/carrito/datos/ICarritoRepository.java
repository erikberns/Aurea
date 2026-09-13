package com.joyeriaEcommerce.AureaTPO.carrito.datos;

import java.util.Map;

/** Contrato interno de almacenamiento, de uso exclusivo del componente Carrito. */
public interface ICarritoRepository {
    Map<Long, Integer> consultar();
    int cantidadDe(Long productoId);
    void guardar(Long productoId, int cantidad);
    void quitar(Long productoId);
    void vaciar();
}
