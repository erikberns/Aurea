package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record OrderDTO(
        Long id,
        String shippingAddress,
        LocalDate orderDate,
        OrderStatus status,
        Double total,
        List<OrderItemDTO> items,
        UsuarioDTO user) {

    public static OrderDTO desde(Order order) {
        if (order == null) {
            return null;
        }
        
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(OrderItemDTO::desde)
                .collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getShippingAddress(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotal(),
                itemDTOs,
                UsuarioDTO.desde(order.getUser())
        );
    }
}

record OrderItemDTO(
        Long id,
        Integer quantity,
        Double price,
        ProductDTO product) {

    public static OrderItemDTO desde(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemDTO(
                item.getId(),
                item.getQuantity(),
                item.getPrice(),
                ProductDTO.desde(item.getProduct())
        );
    }
}
