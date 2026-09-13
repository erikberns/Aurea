package com.joyeriaEcommerce.AureaTPO.carrito.negocio;
import java.util.List;
public record CarritoDTO(List<Item> items, double subtotal, double envio, double total, int cantidadTotal) {
 public record Item(Long id, String nombre, String imagen, double precio, int cantidad, String opcion) {}
}
