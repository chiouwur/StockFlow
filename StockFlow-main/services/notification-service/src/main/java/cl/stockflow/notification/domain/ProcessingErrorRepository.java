package cl.stockflow.notification.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, UUID> { }
