package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.IPagos;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.IUsuarios;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;

/** Facade: coordina la compra mediante contratos de negocio de Usuarios, Productos, Pedidos y Pagos. */
@Service
public class CheckoutFacade implements ICheckout {
    private final IPedidos pedidos;
    private final OrderRepository orderRepository;
    private final IProductos productos;
    private final IUsuarios usuarios;
    private final ICalculoEnvio envio;
    private final IPagos pagos;

    public CheckoutFacade(IPedidos pedidos, OrderRepository orderRepository, IProductos productos,
                          IUsuarios usuarios, ICalculoEnvio envio, IPagos pagos) {
        this.pedidos = pedidos;
        this.orderRepository = orderRepository;
        this.productos = productos;
        this.usuarios = usuarios;
        this.envio = envio;
        this.pagos = pagos;
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

    /**
     * Flujo crítico con transacción declarativa:
     * 1) crea el pedido PENDING; 2) autoriza el pago en la pasarela (solo retiene el importe);
     * 3) confirma el pedido y descuenta stock (Observer síncrono).
     * Si cualquier paso falla, se revierte todo: pedido y stock vuelven atrás y Pagos anula la retención.
     * Recién después del commit se captura el cobro y se publica el evento a RabbitMQ.
     */
    @Override
    @Transactional
    public OrderDTO purchase(String username, String direccion, Map<Long, Integer> items, String metodoPago) {
        OrderDTO creada = placeOrder(username, direccion, items);
        pagos.autorizar(creada.id(), creada.total(), metodoPago);
        return pedidos.confirmOrder(creada.id(), username);
    }
}
