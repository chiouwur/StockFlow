package cl.stockflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

// Registro de mensajes que no se pudieron procesar y fueron enviados a la DLQ.
@Entity
@Table(name = "processing_errors")
public class ProcessingError {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    protected ProcessingError() { }

    public ProcessingError(String payload, String errorMessage) {
        this.id = UUID.randomUUID();
        this.date = Instant.now();
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public UUID getId() { return id; }
    public Instant getDate() { return date; }
    public String getPayload() { return payload; }
    public String getErrorMessage() { return errorMessage; }
}
