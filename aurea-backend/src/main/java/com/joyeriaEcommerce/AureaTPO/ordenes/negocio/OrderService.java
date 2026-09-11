package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import com.joyeriaEcommerce.AureaTPO.ordenes.presentacion.CreateOrderRequest;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductRepository;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UsuarioRepository usuarioRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request, String username) {
        Usuario user = usuarioRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Order order = new Order(request.getShippingAddress(), LocalDate.now(), OrderStatus.PENDING, 0.0, user);
        double total = 0.0;

        for (CreateOrderRequest.CreateOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + product.getName());
            }

            double price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            OrderItem orderItem = new OrderItem(order, product, itemRequest.getQuantity(), price);
            order.addItem(orderItem);
            
            total += price * itemRequest.getQuantity();
        }

        order.setTotal(total);
        return orderRepository.save(order);
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

        return savedOrder;
    }

    @Transactional
    public Order approveReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
