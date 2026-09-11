package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductImage;

public record ProductImageDTO(Long id, Long productId, String imageUrl, Integer position) {

    public static ProductImageDTO desde(ProductImage image) {
        if (image == null) {
            return null;
        }
        return new ProductImageDTO(
                image.getId(),
                image.getProduct() != null ? image.getProduct().getId() : null,
                image.getImageUrl(),
                image.getPosition()
        );
    }
}
