package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;
import com.joyeriaEcommerce.AureaTPO.notificaciones.datos.Notificacion;
import com.joyeriaEcommerce.AureaTPO.notificaciones.datos.NotificacionRepository;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionRecibida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class NotificacionService implements INotificaciones {
    private final NotificacionRepository repository;
    public NotificacionService(NotificacionRepository repository) { this.repository = repository; }
    @Transactional
    public void registrarRecepcion(ReposicionRecibida evento) {
        if (repository.existsById(evento.eventId())) return;
        // La PK evita duplicados concurrentes; una colisión se reintenta y encuentra el aviso ya guardado.
        repository.saveAndFlush(new Notificacion(evento.eventId(),
                "Recepción registrada: " + evento.cantidad() + " unidades del producto #" + evento.productoId()
                        + ". Reposición " + evento.reposicionId() + ".", evento.ocurrida()));
    }
    @Transactional(readOnly = true)
    public List<NotificacionDTO> listar() {
        return repository.findTop100ByOrderByCreadaDesc().stream()
                .map(n -> new NotificacionDTO(n.id, n.mensaje, n.creada)).toList();
    }
}
