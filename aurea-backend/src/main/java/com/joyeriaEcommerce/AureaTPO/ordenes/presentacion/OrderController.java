package com.joyeriaEcommerce.AureaTPO.ordenes.presentacion;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.IPedidos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderDTO;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICheckout;

@RestController
@RequestMapping("/api/ordenes")
public class OrderController {

    private final IPedidos orderService;
    private final ICheckout checkoutFacade;

    public OrderController(IPedidos orderService, ICheckout checkoutFacade) {
        this.orderService = orderService;
        this.checkoutFacade = checkoutFacade;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody @jakarta.validation.Valid CreateOrderRequest request, Authentication authentication) {
        String username = authentication.getName();
        java.util.Map<Long, Integer> itemsMap = new java.util.HashMap<>();
        for (CreateOrderRequest.CreateOrderItemRequest item : request.getItems()) {
            if(itemsMap.putIfAbsent(item.getProductId(), item.getQuantity())!=null) throw new IllegalArgumentException("Producto repetido");
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
