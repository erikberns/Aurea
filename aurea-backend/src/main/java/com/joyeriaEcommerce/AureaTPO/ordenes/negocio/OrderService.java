package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.eventos.OrderConfirmedEvent;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.IUsuarios;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService implements IPedidos {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final IUsuarios usuarios;
    private final IProductos productos;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher,
                        IUsuarios usuarios, IProductos productos) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.usuarios = usuarios;
        this.productos = productos;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO consultarPedido(Long id) {
        return toDTO(buscar(id));
    }

    @Override
    @Transactional
    public OrderDTO confirmOrder(Long orderId, String username) {
        Order order = buscar(orderId);
        Long usuarioId = usuarios.consultarPerfilPorEmail(username).id();
        if (!order.getUserId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para confirmar esta orden");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("La orden no está en un estado válido para confirmar");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        Map<Long, Integer> cantidades = new HashMap<>();
        for (var item : saved.getItems()) {
            cantidades.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        // Contrato de evento, síncrono: un fallo de stock revierte la confirmación.
        eventPublisher.publishEvent(new OrderConfirmedEvent(saved.getId(), cantidades, saved.getUserId()));
        return toDTO(saved);
    }

    @Override
    @Transactional
    public OrderDTO approveReturn(Long id) {
        Order order = buscar(id);
        order.setStatus(OrderStatus.CANCELLED);
        return toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUser(String username) {
        Long usuarioId = usuarios.consultarPerfilPorEmail(username).id();
        return orderRepository.findByUserIdOrderByOrderDateDesc(usuarioId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"))
                .stream().map(this::toDTO).toList();
    }

    private Order buscar(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
    }

    private OrderDTO toDTO(Order order) {
        var items = order.getItems().stream()
                .map(item -> new OrderDTO.Item(item.getId(), item.getQuantity(), item.getPrice(),
                        productos.getProductById(item.getProductId())))
                .toList();
        return new OrderDTO(order.getId(), order.getShippingAddress(), order.getOrderDate(),
                order.getStatus().name(), order.getTotal(), items, usuarios.consultarPerfil(order.getUserId()));
    }
}
