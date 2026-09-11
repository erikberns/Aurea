package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

public record CreateProductRequest(
        String name,
        String description,
        Double price,
        Integer stock,
        Long categoryId
) {}
