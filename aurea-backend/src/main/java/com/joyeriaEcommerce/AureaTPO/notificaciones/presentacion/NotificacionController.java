package com.joyeriaEcommerce.AureaTPO.notificaciones.presentacion;
import com.joyeriaEcommerce.AureaTPO.notificaciones.negocio.INotificaciones;
import com.joyeriaEcommerce.AureaTPO.notificaciones.negocio.NotificacionDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasRole('ADMIN')")
public class NotificacionController {
    private final INotificaciones notificaciones;
    public NotificacionController(INotificaciones notificaciones) { this.notificaciones = notificaciones; }
    @GetMapping public List<NotificacionDTO> listar() { return notificaciones.listar(); }
}
