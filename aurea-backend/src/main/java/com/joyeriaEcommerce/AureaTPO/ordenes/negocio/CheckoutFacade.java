package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderItem;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductDAO;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy.CostoEnvio;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.PrecioProducto;

@Service
public class CheckoutFacade {

    private final OrderService orderService;
    private final ProductDAO productDAO;
    private final UsuarioRepository usuarioRepository;
    private final CostoEnvio costoEnvio;

    public CheckoutFacade(OrderService orderService, ProductDAO productDAO, UsuarioRepository usuarioRepository, CostoEnvio costoEnvio) {
        this.orderService = orderService;
        this.productDAO = productDAO;
        this.usuarioRepository = usuarioRepository;
        this.costoEnvio = costoEnvio;
    }

    @Transactional
    public OrderDTO placeOrder(String username, String shippingAddress, Map<Long, Integer> items) {
        if(shippingAddress==null||shippingAddress.isBlank()||items==null||items.isEmpty()) throw new IllegalArgumentException("Direccion y carrito son obligatorios");
        Usuario user = usuarioRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Order order = new Order(shippingAddress, LocalDate.now(), OrderStatus.PENDING, 0.0, user);
        double cartTotal = 0.0;

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            if(productId==null||quantity==null||quantity<=0) throw new IllegalArgumentException("Cantidad invalida");
            Product product = productDAO.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));

            if(!Boolean.TRUE.equals(product.getActive())) throw new IllegalStateException("Producto inactivo");
            if (product.getStock() < quantity) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + product.getName());
            }

            double price = PrecioProducto.finalPrice(product);
            OrderItem orderItem = new OrderItem(order, product, quantity, price);
            order.addItem(orderItem);
            
            cartTotal += price * quantity;
        }

        double shippingCost = costoEnvio.calcular(cartTotal);
        order.setTotal(cartTotal + shippingCost);

        Order savedOrder = orderService.saveOrder(order);
        return OrderDTO.desde(savedOrder);
    }

    @Transactional
    public OrderDTO purchase(String usuario,String direccion,Map<Long,Integer> items){
        OrderDTO creada=placeOrder(usuario,direccion,items);
        return orderService.confirmOrder(creada.id(),usuario);
    }
}
