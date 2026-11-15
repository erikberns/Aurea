package com.joyeriaEcommerce.AureaTPO.productos.negocio;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionRecibida;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "app.messaging.reposiciones.enabled", havingValue = "true", matchIfMissing = true)
public class ReposicionInventarioListener {
    private final IProductos productos;
    private final ObjectMapper json;
    public ReposicionInventarioListener(IProductos productos, ObjectMapper json) { this.productos = productos; this.json = json; }
    @RabbitListener(queues = "aurea.inventario.reposiciones")
    public void recibir(String payload) {
        var evento = json.readValue(payload, ReposicionRecibida.class);
        productos.aplicarReposicion(evento.eventId(), evento.productoId(), evento.cantidad());
    }
}
