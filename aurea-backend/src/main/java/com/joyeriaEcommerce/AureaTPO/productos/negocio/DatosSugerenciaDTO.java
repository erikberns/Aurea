package com.joyeriaEcommerce.AureaTPO.productos.negocio;

/** Datos que carga el administrador para pedir una descripción. Son solo datos del producto, nunca personales. */
public record DatosSugerenciaDTO(String nombre, String material, String detalles) {
}
