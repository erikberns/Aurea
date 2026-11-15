package com.joyeriaEcommerce.AureaTPO.pagos.datos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByOrderByCreadoDesc();
    List<Pago> findByOrdenIdOrderByCreadoDesc(Long ordenId);
}
