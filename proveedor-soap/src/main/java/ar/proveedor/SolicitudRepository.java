package ar.proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitudRepository extends JpaRepository<Solicitud, String> {}
