package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductDAO;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
public class ProductService {

    private final ProductDAO productDAO;
    private final com.joyeriaEcommerce.AureaTPO.categorias.datos.CategoryRepository categoryRepository;

    public ProductService(ProductDAO productDAO, com.joyeriaEcommerce.AureaTPO.categorias.datos.CategoryRepository categoryRepository) {
        this.productDAO = productDAO;
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    public void checkDatabaseConnection() {
        long count = productDAO.count();
        System.out.println("[LIFECYCLE] ProductService montado. Total de productos cargados en memoria/DB: " + count);
    }

    public java.util.List<ProductDTO> getAllProducts() {
        return productDAO.findAll().stream().map(ProductDTO::desde).toList();
    }

    public ProductDTO getProductById(Long id) {
        Product product = productDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        return ProductDTO.desde(product);
    }

    @Transactional
    public ProductDTO createProduct(String name, String description, Double price, Integer stock, Long categoryId, String imageUrl) {
        com.joyeriaEcommerce.AureaTPO.categorias.datos.Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        }
        Product newProduct = new Product(true, description, name, price, imageUrl, stock, category, null);
        return ProductDTO.desde(productDAO.save(newProduct));
    }

    @Transactional
    public ProductDTO updatePrice(Long productId, Double newPrice) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setPrice(newPrice);
        return ProductDTO.desde(productDAO.save(product));
    }

    @Transactional
    public ProductDTO updateProduct(Long productId, String name, String description, String imageUrl) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (name != null && !name.trim().isEmpty()) {
            product.setName(name);
        }
        if (description != null && !description.trim().isEmpty()) {
            product.setDescription(description);
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImageUrl(imageUrl);
        }
        return ProductDTO.desde(productDAO.save(product));
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!productDAO.existsById(productId)) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        productDAO.deleteById(productId);
    }

    @Transactional
    public ProductDTO updateStock(Long productId, Integer newStock) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setStock(newStock);
        return ProductDTO.desde(productDAO.save(product));
    }

    @EventListener
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        System.out.println("Evento recibido en Inventario. Procesando descuento de stock para orden " + event.orderId());
        
        if (event.productQuantities() != null) {
            for (java.util.Map.Entry<Long, Integer> entry : event.productQuantities().entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();
                
                Product product = productDAO.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));
                
                if (product.getStock() < quantity) {
                    throw new IllegalStateException("Stock insuficiente para el producto " + product.getId());
                }
                
                product.setStock(product.getStock() - quantity);
                productDAO.save(product);
                System.out.println("Stock actualizado para producto " + productId + ". Nuevo stock: " + product.getStock());
            }
        }
    }
}
