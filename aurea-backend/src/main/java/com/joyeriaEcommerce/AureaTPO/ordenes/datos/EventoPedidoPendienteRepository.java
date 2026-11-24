package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventoPedidoPendienteRepository extends JpaRepository<EventoPedidoPendiente, String> {
    List<EventoPedidoPendiente> findTop20ByPublicadoIsNullAndProximoIntentoLessThanEqualOrderByCreadoAsc(Instant ahora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoPedidoPendiente e where e.id = :id")
    Optional<EventoPedidoPendiente> bloquear(String id);
}
