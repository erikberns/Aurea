package com.joyeriaEcommerce.AureaTPO.productos.datos;

import java.util.List;
import java.util.Optional;

public interface ProductDAO {
    List<Product> findAll();
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);
    long count();
    boolean existsById(Long id);
}
