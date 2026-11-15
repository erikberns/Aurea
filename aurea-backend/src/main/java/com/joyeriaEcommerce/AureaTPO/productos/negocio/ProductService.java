package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
public class ProductService implements IProductos {

    private final ProductDAO productDAO;
    private final com.joyeriaEcommerce.AureaTPO.productos.datos.CategoryRepository categoryRepository;
    private final com.joyeriaEcommerce.AureaTPO.productos.datos.ReposicionAplicadaRepository reposicionesAplicadas;

    public ProductService(ProductDAO productDAO, com.joyeriaEcommerce.AureaTPO.productos.datos.CategoryRepository categoryRepository,
            com.joyeriaEcommerce.AureaTPO.productos.datos.ReposicionAplicadaRepository reposicionesAplicadas) {
        this.productDAO = productDAO;
        this.categoryRepository = categoryRepository;
        this.reposicionesAplicadas = reposicionesAplicadas;
    }

    @PostConstruct
    public void checkDatabaseConnection() {
        long count = productDAO.count();
        System.out.println("[LIFECYCLE] ProductService montado. Total de productos cargados en memoria/DB: " + count);
    }

    @Transactional(readOnly=true)
    public java.util.List<ProductDTO> getAllProducts() {
        return productDAO.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly=true)
    public ProductDTO getProductById(Long id) {
        Product product = productDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        return toDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(String name, String description, Double price, Integer stock, Long categoryId, String imageUrl) {
        if(name==null||name.isBlank()) throw new IllegalArgumentException("Nombre obligatorio");
        validarPrecio(price); validarStock(stock);
        com.joyeriaEcommerce.AureaTPO.productos.datos.Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        }
        Product newProduct = new Product(true, description, name, price, imageUrl, stock, category);
        return toDTO(productDAO.save(newProduct));
    }

    @Transactional
    public ProductDTO updatePrice(Long productId, Double newPrice) {
        validarPrecio(newPrice);
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setPrice(newPrice);
        return toDTO(productDAO.save(product));
    }

    @Transactional
    public ProductDTO updateProduct(Long productId, String name, String description, String imageUrl, Long categoryId) {
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
        if (categoryId != null) {
            product.setCategory(categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada")));
        }
        return toDTO(productDAO.save(product));
    }

    @Override
    @Transactional
    public ProductDTO cambiarEstado(Long productId, boolean activo) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setActive(activo);
        return toDTO(productDAO.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CategoryDTO> getCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(c.getId(), c.getDescription()))
                .sorted(java.util.Comparator.comparing(CategoryDTO::description))
                .toList();
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
        validarStock(newStock);
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        product.setStock(newStock);
        return toDTO(productDAO.save(product));
    }

    @Override
    @Transactional
    public void descontarStock(java.util.Map<Long, Integer> cantidades) {
        if (cantidades == null || cantidades.isEmpty()) {
            throw new IllegalArgumentException("Productos obligatorios");
        }
        for (var entry : cantidades.entrySet()) {
            Integer cantidad = entry.getValue();
            if (entry.getKey() == null || cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida");
            }
            Product product = productDAO.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            if (!Boolean.TRUE.equals(product.getActive()) || product.getStock() < cantidad) {
                throw new IllegalStateException("Stock insuficiente o producto inactivo: " + product.getId());
            }
            product.setStock(product.getStock() - cantidad);
            productDAO.save(product);
        }
    }

    @Override
    @Transactional
    public void aplicarReposicion(String eventId, Long productoId, int cantidad) {
        java.util.UUID.fromString(eventId);
        if (cantidad <= 0 || cantidad > 10000) throw new IllegalArgumentException("Cantidad de reposición inválida");
        Product product = productDAO.findByIdForUpdate(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto de reposición inexistente"));
        var previa = reposicionesAplicadas.findById(eventId);
        if (previa.isPresent()) {
            if (!previa.get().productoId.equals(productoId) || previa.get().cantidad != cantidad)
                throw new IllegalStateException("Evento reutilizado con otros datos");
            return;
        }
        product.setStock(Math.addExact(product.getStock(), cantidad));
        productDAO.save(product);
        reposicionesAplicadas.saveAndFlush(new com.joyeriaEcommerce.AureaTPO.productos.datos.ReposicionAplicada(eventId, productoId, cantidad));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reposicionAplicada(String eventId) { return reposicionesAplicadas.existsById(eventId); }

    private ProductDTO toDTO(Product product) {
        var category = product.getCategory();
        CategoryDTO categoria = category == null ? null
                : new CategoryDTO(category.getId(), category.getDescription());
        return new ProductDTO(product.getId(), product.getActive(), product.getDescription(),
                product.getName(), product.getPrice(), product.getImageUrl(), product.getStock(), categoria);
    }

    private void validarPrecio(Double p){if(p==null||!Double.isFinite(p)||p<0) throw new IllegalArgumentException("Precio invalido");}
    private void validarStock(Integer s){if(s==null||s<0) throw new IllegalArgumentException("Stock invalido");}
    @jakarta.annotation.PreDestroy
    public void destruir(){System.out.println("[CICLO DE VIDA] ProductService stateless destruido");}
}
