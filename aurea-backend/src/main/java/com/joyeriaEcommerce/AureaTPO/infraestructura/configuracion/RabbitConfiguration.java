package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // publica periódicamente el outbox de pedidos
@ConditionalOnProperty(name = "app.messaging.order-processing.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfiguration {
    public static final String ORDERS_EXCHANGE = "aurea.orders";
    public static final String ORDER_CONFIRMED_QUEUE = "aurea.orders.confirmed";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDERS_DLX = "aurea.orders.dlx";
    public static final String ORDER_CONFIRMED_DLQ = "aurea.orders.confirmed.dlq";

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange ordersDeadLetterExchange() {
        return new DirectExchange(ORDERS_DLX, true, false);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .deadLetterExchange(ORDERS_DLX)
                .deadLetterRoutingKey(ORDER_CONFIRMED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderConfirmedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderConfirmedQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(orderConfirmedQueue)
                .to(ordersExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    Binding orderConfirmedDlqBinding(Queue orderConfirmedDeadLetterQueue, DirectExchange ordersDeadLetterExchange) {
        return BindingBuilder.bind(orderConfirmedDeadLetterQueue)
                .to(ordersDeadLetterExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }
}
