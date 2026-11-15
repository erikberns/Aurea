package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.messaging.reposiciones.enabled", havingValue = "true", matchIfMissing = true)
public class ReposicionMessagingConfig {
    public static final String EXCHANGE = "aurea.reposiciones";
    public static final String KEY = "reposicion.recibida";
    @Bean
    Declarables reposicionTopology() {
        TopicExchange topic = new TopicExchange(EXCHANGE, true, false);
        DirectExchange errores = new DirectExchange(EXCHANGE + ".errores", true, false);
        Queue inventario = cola("aurea.inventario.reposiciones", errores.getName());
        Queue notificaciones = cola("aurea.notificaciones.reposiciones", errores.getName());
        Queue inventarioError = QueueBuilder.durable(inventario.getName() + ".dlq").build();
        Queue notificacionesError = QueueBuilder.durable(notificaciones.getName() + ".dlq").build();
        return new Declarables(topic, errores, inventario, notificaciones, inventarioError, notificacionesError,
                BindingBuilder.bind(inventario).to(topic).with(KEY),
                BindingBuilder.bind(notificaciones).to(topic).with(KEY),
                BindingBuilder.bind(inventarioError).to(errores).with(inventario.getName()),
                BindingBuilder.bind(notificacionesError).to(errores).with(notificaciones.getName()));
    }
    private Queue cola(String nombre, String exchangeError) {
        return QueueBuilder.durable(nombre).deadLetterExchange(exchangeError).deadLetterRoutingKey(nombre).build();
    }
}
