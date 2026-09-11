package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order confirmOrder(Long orderId, String username) {
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

        // Disparar evento de confirmación para inventario y notificaciones
        eventPublisher.publishEvent(new OrderConfirmedEvent(
                savedOrder.getId(),
                savedOrder.getProduct().getId(),
                savedOrder.getQuantity(),
                savedOrder.getUser().getId()
        ));

        return savedOrder;
    }

    @Transactional
    public Order approveReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        // Simplificado: asume que la orden estaba entregada y se aprueba devolución
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
