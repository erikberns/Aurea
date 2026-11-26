package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.CategoryDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.EstadoAsistenteIADTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ISugerenciasProducto;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.SugerenciaProductoDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductController {

    private final IProductos productService;
    private final ISugerenciasProducto sugerencias;

    public ProductController(IProductos productService, ISugerenciasProducto sugerencias) {
        this.productService = productService;
        this.sugerencias = sugerencias;
    }

    /**
     * Asistente de descripciones con IA (Clase 14). Solo sugiere: no guarda nada. Si la IA no está
     * disponible o su respuesta no pasa las validaciones, responde igual con una plantilla (fuente "plantilla").
     */
    @PostMapping("/sugerencia-ia")
    @PreAuthorize("hasRole('ADMIN')")
    public SugerenciaProductoDTO sugerirDescripcion(@Valid @RequestBody SugerenciaIARequest request,
                                                    Authentication auth) {
        return sugerencias.sugerirDescripcion(request.toDatos(), auth.getName());
    }

    @GetMapping("/sugerencia-ia/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public EstadoAsistenteIADTO estadoAsistente() {
        return sugerencias.estado();
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
    public ResponseEntity<ProductDTO> getProductById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        ProductDTO product = productService.getProductById(id);
        String etag = etagDe(product);
        if (coincideEtag(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(product);
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
        return ResponseEntity.status(201).eTag(etagDe(newProduct)).body(newProduct);
    }

    @PatchMapping("/{id}/precio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePrice(
            @PathVariable Long id,
            @RequestParam Double newPrice,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ResponseEntity<ProblemDetail> fallo = verificarIfMatch(id, ifMatch, "/api/productos/" + id + "/precio");
        if (fallo != null) return fallo;
        ProductDTO updated = productService.updatePrice(id, newPrice);
        return ResponseEntity.ok().eTag(etagDe(updated)).body(updated);
    }

    @PatchMapping("/{id}/inventario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStock(
            @PathVariable Long id,
            @RequestParam Integer newStock,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ResponseEntity<ProblemDetail> fallo = verificarIfMatch(id, ifMatch, "/api/productos/" + id + "/inventario");
        if (fallo != null) return fallo;
        ProductDTO updated = productService.updateStock(id, newStock);
        return ResponseEntity.ok().eTag(etagDe(updated)).body(updated);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody CreateProductRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ResponseEntity<ProblemDetail> fallo = verificarIfMatch(id, ifMatch, "/api/productos/" + id);
        if (fallo != null) return fallo;
        ProductDTO updated = productService.updateProduct(id, request.name(), request.description(),
                request.imageUrl(), request.categoryId());
        return ResponseEntity.ok().eTag(etagDe(updated)).body(updated);
    }

    /** Archivar (activo=false) oculta la joya de la tienda sin perder el historial de pedidos. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ResponseEntity<ProblemDetail> fallo = verificarIfMatch(id, ifMatch, "/api/productos/" + id + "/estado");
        if (fallo != null) return fallo;
        ProductDTO updated = productService.cambiarEstado(id, activo);
        return ResponseEntity.ok().eTag(etagDe(updated)).body(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ProblemDetail> verificarIfMatch(Long id, String ifMatch, String instanceUri) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        ProductDTO actual = productService.getProductById(id);
        String etagActual = etagDe(actual);
        if (coincideEtag(ifMatch, etagActual)) {
            return null;
        }
        String detalle = "El recurso fue modificado. ETag esperado: " + etagActual + ".";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, detalle);
        pd.setType(URI.create("https://aurea.local/problemas/precondicion-fallida"));
        pd.setTitle("Precondición fallida (ETag desactualizado)");
        pd.setInstance(URI.create(instanceUri));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("mensaje", detalle);
        pd.setProperty("errores", Map.of());
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .eTag(etagActual)
                .body(pd);
    }

    private static String etagDe(ProductDTO p) {
        int version = p.version() == null ? 0 : p.version();
        int hash = Objects.hash(p.id(), version, p.active(), p.name(), p.description(), p.price(), p.stock(),
                p.imageUrl(), p.category() == null ? null : p.category().id());
        return "\"p" + p.id() + "-v" + version + "-" + Integer.toHexString(hash) + "\"";
    }

    private static boolean coincideEtag(String header, String etagActual) {
        if (header == null || header.isBlank()) return false;
        String normalizadoActual = quitarDebil(etagActual);
        for (String candidato : header.split(",")) {
            String limpio = candidato.trim();
            if ("*".equals(limpio) || normalizadoActual.equals(quitarDebil(limpio))) {
                return true;
            }
        }
        return false;
    }

    private static String quitarDebil(String etag) {
        return etag.startsWith("W/") ? etag.substring(2).trim() : etag.trim();
    }
}
