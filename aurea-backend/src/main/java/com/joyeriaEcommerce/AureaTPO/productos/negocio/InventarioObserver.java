package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.eventos.OrderConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Observer síncrono: el descuento de stock participa en la transacción de compra. */
@Component
public class InventarioObserver {
    private final IProductos productos;

    public InventarioObserver(IProductos productos) {
        this.productos = productos;
    }

    @EventListener
    public void alConfirmar(OrderConfirmedEvent event) {
        productos.descontarStock(event.productQuantities());
    }
}
