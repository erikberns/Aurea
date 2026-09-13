package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import java.util.List;

/** Operaciones públicas de pedidos, expresadas únicamente con identificadores y DTO. */
public interface IPedidos {
    OrderDTO consultarPedido(Long id);
    OrderDTO confirmOrder(Long id, String username);
    OrderDTO approveReturn(Long id);
    List<OrderDTO> getOrdersByUser(String username);
    List<OrderDTO> getAllOrders();
}
