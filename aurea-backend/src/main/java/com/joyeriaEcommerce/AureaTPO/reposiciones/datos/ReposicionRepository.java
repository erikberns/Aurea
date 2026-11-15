package com.joyeriaEcommerce.AureaTPO.reposiciones.datos;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ReposicionRepository extends JpaRepository<Reposicion, String> {
    List<Reposicion> findAllByOrderByCreadaDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reposicion r where r.id = :id")
    Optional<Reposicion> bloquear(String id);
}
