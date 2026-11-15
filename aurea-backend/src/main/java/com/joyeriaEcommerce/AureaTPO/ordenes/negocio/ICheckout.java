package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import java.util.Map;

/** Fachada pública del proceso de compra. */
public interface ICheckout {
    OrderDTO placeOrder(String username, String direccion, Map<Long, Integer> items);
    /** Crea el pedido, autoriza el pago, confirma y descuenta stock en una única transacción. */
    OrderDTO purchase(String username, String direccion, Map<Long, Integer> items, String metodoPago);
}
