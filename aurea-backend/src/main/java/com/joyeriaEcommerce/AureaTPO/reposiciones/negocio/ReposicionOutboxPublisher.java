package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.EventoPendienteRepository;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.ReposicionMessagingConfig;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.MessageDeliveryMode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "app.messaging.reposiciones.enabled", havingValue = "true", matchIfMissing = true)
public class ReposicionOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(ReposicionOutboxPublisher.class);
    private final EventoPendienteRepository eventos;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    public ReposicionOutboxPublisher(EventoPendienteRepository eventos, RabbitTemplate rabbit, PlatformTransactionManager manager) {
        this.eventos = eventos; this.rabbit = rabbit; this.tx = new TransactionTemplate(manager);
    }
    @Scheduled(fixedDelayString = "${app.messaging.outbox.delay-ms:3000}")
    public void publicarPendientes() {
        for (var candidato : eventos.findTop20ByPublicadoIsNullAndProximoIntentoLessThanEqualOrderByCreadoAsc(Instant.now())) {
            try {
                tx.executeWithoutResult(s -> {
                    var e = eventos.bloquear(candidato.id).orElseThrow();
                    if (e.publicado != null || e.proximoIntento.isAfter(Instant.now())) return;
                    e.intentos++;
                    try {
                        var correlacion = new CorrelationData(UUID.randomUUID().toString());
                        rabbit.convertAndSend(ReposicionMessagingConfig.EXCHANGE, ReposicionMessagingConfig.KEY, e.payload,
                                message -> {
                                    message.getMessageProperties().setMessageId(e.id);
                                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                                    return message;
                                }, correlacion);
                        var confirm = correlacion.getFuture().get(5, TimeUnit.SECONDS);
                        if (!confirm.ack() || correlacion.getReturned() != null) throw new IllegalStateException("Evento no confirmado o sin ruta");
                        e.publicado = Instant.now(); e.ultimoError = null;
                    } catch (Exception ex) {
                        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                        e.ultimoError = "Publicación pendiente; se reintentará automáticamente";
                        e.proximoIntento = Instant.now().plusSeconds(Math.min(60, e.intentos * 3L));
                        log.warn("Reposición {}: publicación pendiente (intento {})", e.id, e.intentos);
                    }
                });
            } catch (RuntimeException ex) {
                log.warn("No se pudo actualizar outbox de {}; se revisará en el próximo ciclo", candidato.id);
            }
        }
    }
}
