package com.joyeriaEcommerce.AureaTPO.productos.negocio;

/** Datos públicos del catálogo; no contiene ni recibe entidades de persistencia. */
public record ProductDTO(Long id, Boolean active, String description, String name,
                         Double price, String imageUrl, Integer stock, CategoryDTO category) {
}
