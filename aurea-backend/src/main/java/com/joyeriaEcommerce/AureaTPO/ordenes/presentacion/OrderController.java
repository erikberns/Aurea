package com.joyeriaEcommerce.AureaTPO.ordenes.presentacion;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderDTO;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.CheckoutFacade;

@RestController
@RequestMapping("/api/ordenes")
public class OrderController {

    private final OrderService orderService;
    private final CheckoutFacade checkoutFacade;

    public OrderController(OrderService orderService, CheckoutFacade checkoutFacade) {
        this.orderService = orderService;
        this.checkoutFacade = checkoutFacade;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody @jakarta.validation.Valid CreateOrderRequest request, Authentication authentication) {
        String username = authentication.getName();
        java.util.Map<Long, Integer> itemsMap = new java.util.HashMap<>();
        for (CreateOrderRequest.CreateOrderItemRequest item : request.getItems()) {
            itemsMap.put(item.getProductId(), item.getQuantity());
        }
        return ResponseEntity.ok(checkoutFacade.placeOrder(username, request.getShippingAddress(), itemsMap));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<OrderDTO> confirmOrder(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.confirmOrder(id, username));
    }

    @PostMapping("/{id}/devolucion/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> approveReturn(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveReturn(id));
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<java.util.List<OrderDTO>> getMyOrders(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(username));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
