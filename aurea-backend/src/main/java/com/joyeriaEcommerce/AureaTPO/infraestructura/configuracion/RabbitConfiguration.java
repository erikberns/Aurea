package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "app.messaging.order-processing.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfiguration {
    public static final String ORDERS_EXCHANGE = "aurea.orders";
    public static final String ORDER_CONFIRMED_QUEUE = "aurea.orders.confirmed";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";

    @Bean
    DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE).build();
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderConfirmedQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(orderConfirmedQueue)
                .to(ordersExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }
}