package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.categorias.negocio.CategoryDTO;
import com.joyeriaEcommerce.AureaTPO.descuentos.negocio.DiscountDTO;

public record ProductDTO(
        Long id,
        Boolean active,
        String description,
        String name,
        Double price,
        Double discountPrice,
        Integer stock,
        CategoryDTO category,
        DiscountDTO discount) {

    public static ProductDTO desde(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDTO(
                product.getId(),
                product.getActive(),
                product.getDescription(),
                product.getName(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getStock(),
                CategoryDTO.desde(product.getCategory()),
                DiscountDTO.desde(product.getDiscount())
        );
    }
}
