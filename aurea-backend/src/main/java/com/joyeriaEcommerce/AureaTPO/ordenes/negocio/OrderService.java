package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }



    @Transactional
    public OrderDTO confirmOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!order.getUser().getEmail().equals(username)) {
            throw new SecurityException("No tienes permiso para confirmar esta orden");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("La orden no está en un estado válido para confirmar");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);


        Map<Long, Integer> productQuantities = new HashMap<>();
        for (OrderItem item : savedOrder.getItems()) {
            productQuantities.put(item.getProduct().getId(), item.getQuantity());
        }

        // Disparar evento de confirmación para inventario y notificaciones
        eventPublisher.publishEvent(new OrderConfirmedEvent(
                savedOrder.getId(),
                productQuantities,
                savedOrder.getUser().getId()
        ));

        return OrderDTO.desde(savedOrder);
    }

    @Transactional
    public OrderDTO approveReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        order.setStatus(OrderStatus.CANCELLED);
        return OrderDTO.desde(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderDTO> getOrdersByUser(String username) {
        return orderRepository.findByUser_EmailOrderByOrderDateDesc(username).stream().map(OrderDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderDTO> getAllOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "orderDate")).stream().map(OrderDTO::desde).toList();
    }
}
