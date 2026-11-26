package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrazaIARepository extends JpaRepository<TrazaIA, Long> {
    List<TrazaIA> findTop200ByOrderByCreadaDesc();
}
