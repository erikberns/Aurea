package com.joyeriaEcommerce.AureaTPO.reposiciones.datos;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface EventoPendienteRepository extends JpaRepository<EventoPendiente, String> {
    List<EventoPendiente> findTop20ByPublicadoIsNullAndProximoIntentoLessThanEqualOrderByCreadoAsc(Instant ahora);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoPendiente e where e.id = :id")
    Optional<EventoPendiente> bloquear(String id);
}
