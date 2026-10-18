package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.order-processing.enabled", havingValue = "true", matchIfMissing = true)
public class OrderProcessingListener {
    private final IPedidos pedidos;

    public OrderProcessingListener(IPedidos pedidos) {
        this.pedidos = pedidos;
    }

    @RabbitListener(queues = RabbitConfiguration.ORDER_CONFIRMED_QUEUE)
    public void processConfirmedOrder(String orderId) {
        pedidos.completeOrder(Long.valueOf(orderId));
    }
}