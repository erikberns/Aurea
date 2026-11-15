package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionRecibida;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.ObjectMapper;
@Component
@ConditionalOnProperty(name = "app.messaging.reposiciones.enabled", havingValue = "true", matchIfMissing = true)
public class ReposicionNotificacionListener {
    private final INotificaciones notificaciones;
    private final ObjectMapper json;
    public ReposicionNotificacionListener(INotificaciones notificaciones, ObjectMapper json) {
        this.notificaciones = notificaciones; this.json = json;
    }
    @RabbitListener(queues = "aurea.notificaciones.reposiciones")
    public void recibir(String payload) {
        notificaciones.registrarRecepcion(json.readValue(payload, ReposicionRecibida.class));
    }
}
