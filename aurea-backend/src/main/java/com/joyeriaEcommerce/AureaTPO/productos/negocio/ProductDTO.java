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
        String imageUrl,
        Integer stock,
        CategoryDTO category,
        DiscountDTO discount) {

    public static ProductDTO desde(Product product) {
        if (product == null) {
            return null;
        }
        Double calculatedDiscountPrice = null;
        if (product.getDiscount() != null && Boolean.TRUE.equals(product.getDiscount().getActive())) {
            calculatedDiscountPrice = product.getPrice() * (1 - product.getDiscount().getPercentage() / 100);
        } else if (product.getCategory() != null && product.getCategory().getDiscount() != null && Boolean.TRUE.equals(product.getCategory().getDiscount().getActive())) {
            calculatedDiscountPrice = product.getPrice() * (1 - product.getCategory().getDiscount().getPercentage() / 100);
        }

        return new ProductDTO(
                product.getId(),
                product.getActive(),
                product.getDescription(),
                product.getName(),
                product.getPrice(),
                calculatedDiscountPrice,
                product.getImageUrl(),
                product.getStock(),
                CategoryDTO.desde(product.getCategory()),
                DiscountDTO.desde(product.getDiscount())
        );
    }
}
