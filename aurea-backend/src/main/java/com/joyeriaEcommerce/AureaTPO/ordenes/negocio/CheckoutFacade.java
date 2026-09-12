package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy.FreeShippingStrategy;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy.ShippingStrategy;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy.StandardShippingStrategy;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductRepository;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class CheckoutFacade {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final UsuarioRepository usuarioRepository;
    private final FreeShippingStrategy freeShippingStrategy;
    private final StandardShippingStrategy standardShippingStrategy;

    public CheckoutFacade(OrderService orderService, ProductRepository productRepository, UsuarioRepository usuarioRepository, FreeShippingStrategy freeShippingStrategy, StandardShippingStrategy standardShippingStrategy) {
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.usuarioRepository = usuarioRepository;
        this.freeShippingStrategy = freeShippingStrategy;
        this.standardShippingStrategy = standardShippingStrategy;
    }

    @Transactional
    public OrderDTO placeOrder(String username, String shippingAddress, Map<Long, Integer> items) {
        Usuario user = usuarioRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Order order = new Order(shippingAddress, LocalDate.now(), OrderStatus.PENDING, 0.0, user);
        double cartTotal = 0.0;

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));

            if (product.getStock() < quantity) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + product.getName());
            }

            double price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            OrderItem orderItem = new OrderItem(order, product, quantity, price);
            order.addItem(orderItem);
            
            cartTotal += price * quantity;
        }

        // Apply Shipping Strategy
        ShippingStrategy strategy = (cartTotal >= 60000) ? freeShippingStrategy : standardShippingStrategy;
        double shippingCost = strategy.calculateShippingCost(cartTotal);
        
        order.setTotal(cartTotal + shippingCost);

        Order savedOrder = orderService.saveOrder(order);
        return OrderDTO.desde(savedOrder);
    }
}
