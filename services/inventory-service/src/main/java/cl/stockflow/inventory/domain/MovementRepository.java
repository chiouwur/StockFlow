package cl.stockflow.inventory.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID> {
    List<Movement> findTop200ByOrderByDateDesc();
    List<Movement> findTop200ByProductIdOrderByDateDesc(UUID productId);
}
