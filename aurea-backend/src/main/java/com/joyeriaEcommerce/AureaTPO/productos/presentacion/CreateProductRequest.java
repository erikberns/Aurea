package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta y edición de joyas. Los largos coinciden con las columnas de la base (VARCHAR 255). */
public record CreateProductRequest(
        @Size(max = 255) @Pattern(regexp = "[^\\p{Cntrl}]*", message = "No se admiten caracteres de control") String name,
        @Size(max = 255) String description,
        Double price,
        Integer stock,
        Long categoryId,
        @Size(max = 255) String imageUrl
) {}
