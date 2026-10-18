package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.eventos.OrderConfirmedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.messaging.order-processing.enabled", havingValue = "true", matchIfMissing = true)
public class OrderProcessingPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderProcessingPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfiguration.ORDERS_EXCHANGE,
                RabbitConfiguration.ORDER_CONFIRMED_ROUTING_KEY,
                event.orderId().toString());
    }
}