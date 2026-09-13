package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioDTO;
import java.time.LocalDate;
import java.util.List;

/** Contrato de respuesta. El mapeo y las consultas quedan dentro del servicio. */
public record OrderDTO(Long id, String shippingAddress, LocalDate orderDate, String status,
                       Double total, List<Item> items, UsuarioDTO user) {
    public record Item(Long id, Integer quantity, Double price, ProductDTO product) {}
}
