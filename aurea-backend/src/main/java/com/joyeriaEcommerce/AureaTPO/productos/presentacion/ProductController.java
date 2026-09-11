package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PatchMapping("/{id}/precio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updatePrice(@PathVariable Long id, @RequestParam Double newPrice) {
        return ResponseEntity.ok(productService.updatePrice(id, newPrice));
    }

    @PatchMapping("/{id}/inventario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestParam Integer newStock) {
        return ResponseEntity.ok(productService.updateStock(id, newStock));
    }
}
