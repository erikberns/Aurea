package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import java.util.List;
import java.util.Map;

/** Contrato público de catálogo e inventario. No expone entidades ni repositorios. */
public interface IProductos {
    List<ProductDTO> getAllProducts();
    ProductDTO getProductById(Long id);
    ProductDTO createProduct(String name, String description, Double price, Integer stock, Long categoryId, String imageUrl);
    ProductDTO updatePrice(Long id, Double price);
    ProductDTO updateStock(Long id, Integer stock);
    ProductDTO updateProduct(Long id, String name, String description, String imageUrl);
    void deleteProduct(Long id);
    void descontarStock(Map<Long, Integer> cantidades);
}
