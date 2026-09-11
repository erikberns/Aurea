package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public java.util.List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    @Transactional
    public Product updatePrice(Long productId, Double newPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setPrice(newPrice);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateStock(Long productId, Integer newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setStock(newStock);
        return productRepository.save(product);
    }

    @EventListener
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        System.out.println("Evento recibido en Inventario. Procesando descuento de stock para orden " + event.orderId());
        
        if (event.productQuantities() != null) {
            for (java.util.Map.Entry<Long, Integer> entry : event.productQuantities().entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();
                
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));
                
                if (product.getStock() < quantity) {
                    throw new IllegalStateException("Stock insuficiente para el producto " + product.getId());
                }
                
                product.setStock(product.getStock() - quantity);
                productRepository.save(product);
                System.out.println("Stock actualizado para producto " + productId + ". Nuevo stock: " + product.getStock());
            }
        }
    }
}
