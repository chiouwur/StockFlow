package cl.stockflow.notification.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    boolean existsByEventId(UUID eventId);
    List<Alert> findTop100ByOrderByDateDesc();
}
