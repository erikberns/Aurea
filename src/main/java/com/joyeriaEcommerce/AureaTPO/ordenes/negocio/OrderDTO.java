package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioDTO;
import java.time.LocalDate;

public record OrderDTO(
        Long id,
        Integer quantity,
        String shippingAddress,
        LocalDate orderDate,
        OrderStatus status,
        Double total,
        ProductDTO product,
        UsuarioDTO user) {

    public static OrderDTO desde(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDTO(
                order.getId(),
                order.getQuantity(),
                order.getShippingAddress(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotal(),
                ProductDTO.desde(order.getProduct()),
                UsuarioDTO.desde(order.getUser())
        );
    }
}
