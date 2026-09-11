package com.joyeriaEcommerce.AureaTPO.categorias.negocio;

import com.joyeriaEcommerce.AureaTPO.categorias.datos.Category;
import com.joyeriaEcommerce.AureaTPO.descuentos.negocio.DiscountDTO;

public record CategoryDTO(Long id, String description, DiscountDTO discount) {

    public static CategoryDTO desde(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDTO(
                category.getId(),
                category.getDescription(),
                DiscountDTO.desde(category.getDiscount())
        );
    }
}
