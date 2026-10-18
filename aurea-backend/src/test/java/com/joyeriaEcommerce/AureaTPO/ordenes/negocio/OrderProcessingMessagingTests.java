package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.eventos.OrderConfirmedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderProcessingMessagingTests {
    @Test
    void publishesConfirmedOrderIdToTheConfiguredQueueRoute() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderProcessingPublisher publisher = new OrderProcessingPublisher(rabbitTemplate);

        publisher.onOrderConfirmed(new OrderConfirmedEvent(42L, Map.of(), 7L));

        verify(rabbitTemplate).convertAndSend(
                RabbitConfiguration.ORDERS_EXCHANGE,
                RabbitConfiguration.ORDER_CONFIRMED_ROUTING_KEY,
                "42");
    }

    @Test
    void listenerDelegatesOrderCompletionToTheBusinessContract() {
        IPedidos pedidos = mock(IPedidos.class);
        OrderProcessingListener listener = new OrderProcessingListener(pedidos);

        listener.processConfirmedOrder("42");

        verify(pedidos).completeOrder(42L);
    }
}