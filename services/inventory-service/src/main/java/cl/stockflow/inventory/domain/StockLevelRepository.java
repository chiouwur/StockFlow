package cl.stockflow.inventory.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLevelRepository extends JpaRepository<StockLevel, UUID> {

    // Bloquea la fila para que dos movimientos simultáneos no corrompan el stock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockLevel s where s.productId = :id")
    Optional<StockLevel> findForUpdate(@Param("id") UUID id);
}
