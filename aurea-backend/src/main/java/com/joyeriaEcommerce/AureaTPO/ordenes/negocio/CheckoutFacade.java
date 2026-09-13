package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.IUsuarios;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;

/** Facade: coordina la compra mediante contratos de negocio. */
@Service
public class CheckoutFacade implements ICheckout {
    private final IPedidos pedidos;
    private final OrderRepository orderRepository;
    private final IProductos productos;
    private final IUsuarios usuarios;
    private final ICalculoEnvio envio;

    public CheckoutFacade(IPedidos pedidos, OrderRepository orderRepository, IProductos productos,
                          IUsuarios usuarios, ICalculoEnvio envio) {
        this.pedidos = pedidos;
        this.orderRepository = orderRepository;
        this.productos = productos;
        this.usuarios = usuarios;
        this.envio = envio;
    }

    @Override
    @Transactional
    public OrderDTO placeOrder(String username, String direccion, Map<Long, Integer> items) {
        if (direccion == null || direccion.isBlank() || items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Dirección y carrito son obligatorios");
        }
        var usuario = usuarios.consultarPerfilPorEmail(username);
        Order order = new Order(direccion, LocalDate.now(), OrderStatus.PENDING, 0.0, usuario.id());
        double subtotal = 0;
        for (var entry : items.entrySet()) {
            Long id = entry.getKey();
            Integer cantidad = entry.getValue();
            if (id == null || cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida");
            }
            ProductDTO producto = productos.getProductById(id);
            if (!Boolean.TRUE.equals(producto.active()) || producto.stock() < cantidad) {
                throw new IllegalStateException("Producto inactivo o stock insuficiente: " + producto.name());
            }
            order.addItem(new OrderItem(order, producto.id(), cantidad, producto.price()));
            subtotal += producto.price() * cantidad;
        }
        order.setTotal(subtotal + envio.calcular(subtotal));
        Order guardada = orderRepository.save(order);
        return pedidos.consultarPedido(guardada.getId());
    }

    @Override
    @Transactional
    public OrderDTO purchase(String username, String direccion, Map<Long, Integer> items) {
        OrderDTO creada = placeOrder(username, direccion, items);
        return pedidos.confirmOrder(creada.id(), username);
    }
}
