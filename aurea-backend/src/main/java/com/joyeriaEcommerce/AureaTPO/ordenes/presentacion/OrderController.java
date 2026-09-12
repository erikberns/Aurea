package com.joyeriaEcommerce.AureaTPO.ordenes.presentacion;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordenes")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody @jakarta.validation.Valid CreateOrderRequest request, Authentication authentication) {
        String username = authentication.getName();
        java.util.Map<Long, Integer> itemsMap = new java.util.HashMap<>();
        for (CreateOrderRequest.CreateOrderItemRequest item : request.getItems()) {
            itemsMap.put(item.getProductId(), item.getQuantity());
        }
        return ResponseEntity.ok(orderService.createOrder(request.getShippingAddress(), itemsMap, username));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Order> confirmOrder(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.confirmOrder(id, username));
    }

    @PostMapping("/{id}/devolucion/aprobar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERADOR_INVENTARIO')")
    public ResponseEntity<Order> approveReturn(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveReturn(id));
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<java.util.List<Order>> getMyOrders(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(username));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
