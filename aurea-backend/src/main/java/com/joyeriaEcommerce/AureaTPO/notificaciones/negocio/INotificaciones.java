package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionRecibida;
import java.util.List;
public interface INotificaciones {
    void registrarRecepcion(ReposicionRecibida evento);
    List<NotificacionDTO> listar();
}
