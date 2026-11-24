package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.EventoPedidoPendienteRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica en la cola aurea.orders los pedidos confirmados que esperan en el outbox.
 * Un evento se marca como publicado solo cuando RabbitMQ confirma que lo recibió y lo pudo enrutar;
 * si el broker está caído, el evento queda pendiente y se reintenta con espera creciente.
 * El consumidor es idempotente (completeOrder ignora un pedido ya completado), así que un reenvío no duplica nada.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.order-processing.enabled", havingValue = "true", matchIfMissing = true)
public class OrderProcessingPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingPublisher.class);

    private final EventoPedidoPendienteRepository eventos;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate tx;

    public OrderProcessingPublisher(EventoPedidoPendienteRepository eventos, RabbitTemplate rabbitTemplate,
                                    PlatformTransactionManager manager) {
        this.eventos = eventos;
        this.rabbitTemplate = rabbitTemplate;
        this.tx = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox.delay-ms:3000}")
    public void publicarPendientes() {
        for (var candidato : eventos.findTop20ByPublicadoIsNullAndProximoIntentoLessThanEqualOrderByCreadoAsc(Instant.now())) {
            try {
                tx.executeWithoutResult(estado -> {
                    var e = eventos.bloquear(candidato.id).orElseThrow();
                    if (e.publicado != null || e.proximoIntento.isAfter(Instant.now())) return;
                    e.intentos++;
                    try {
                        var correlacion = new CorrelationData(UUID.randomUUID().toString());
                        rabbitTemplate.convertAndSend(RabbitConfiguration.ORDERS_EXCHANGE,
                                RabbitConfiguration.ORDER_CONFIRMED_ROUTING_KEY, e.payload,
                                message -> {
                                    message.getMessageProperties().setMessageId(e.id);
                                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                                    return message;
                                }, correlacion);
                        var confirmacion = correlacion.getFuture().get(5, TimeUnit.SECONDS);
                        if (!confirmacion.ack() || correlacion.getReturned() != null) {
                            throw new IllegalStateException("Evento no confirmado o sin ruta");
                        }
                        e.publicado = Instant.now();
                        e.ultimoError = null;
                    } catch (Exception ex) {
                        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                        e.ultimoError = "Publicación pendiente; se reintentará automáticamente";
                        e.proximoIntento = Instant.now().plusSeconds(Math.min(60, e.intentos * 3L));
                        log.warn("Pedido {}: publicación pendiente (intento {})", e.payload, e.intentos);
                    }
                });
            } catch (RuntimeException ex) {
                log.warn("No se pudo actualizar el outbox de {}; se revisará en el próximo ciclo", candidato.id);
            }
        }
    }
}
