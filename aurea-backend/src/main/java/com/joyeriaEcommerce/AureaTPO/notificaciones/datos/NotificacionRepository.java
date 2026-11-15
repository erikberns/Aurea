package com.joyeriaEcommerce.AureaTPO.notificaciones.datos;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificacionRepository extends JpaRepository<Notificacion, String> {
    List<Notificacion> findTop100ByOrderByCreadaDesc();
}
