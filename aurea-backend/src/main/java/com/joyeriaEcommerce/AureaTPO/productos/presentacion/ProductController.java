package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.CategoryDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductController {

    private final IProductos productService;

    public ProductController(IProductos productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /** Categorías del catálogo, públicas: las usa la tienda para filtrar y el panel para clasificar. */
    @GetMapping("/categorias")
    public java.util.List<CategoryDTO> getCategories() {
        return productService.getCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody CreateProductRequest request) {
        ProductDTO newProduct = productService.createProduct(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.categoryId(),
                request.imageUrl()
        );
        return ResponseEntity.status(201).body(newProduct);
    }

    @PatchMapping("/{id}/precio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updatePrice(@PathVariable Long id, @RequestParam Double newPrice) {
        return ResponseEntity.ok(productService.updatePrice(id, newPrice));
    }

    @PatchMapping("/{id}/inventario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateStock(@PathVariable Long id, @RequestParam Integer newStock) {
        return ResponseEntity.ok(productService.updateStock(id, newStock));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request.name(), request.description(),
                request.imageUrl(), request.categoryId()));
    }

    /** Archivar (activo=false) oculta la joya de la tienda sin perder el historial de pedidos. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(productService.cambiarEstado(id, activo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
