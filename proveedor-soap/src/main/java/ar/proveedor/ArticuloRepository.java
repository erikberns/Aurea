package ar.proveedor;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Articulo a where a.codigo = :codigo")
    Optional<Articulo> bloquear(String codigo);
}
