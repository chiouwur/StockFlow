package cl.stockflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    // Un evento solo puede generar una alerta: evita duplicados si RabbitMQ lo entrega dos veces.
    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 20)
    private String status;

    protected Alert() { }

    public Alert(UUID productId, UUID eventId, String message, Instant date) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.eventId = eventId;
        this.message = message;
        this.date = date;
        this.status = "OPEN";
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public UUID getEventId() { return eventId; }
    public String getMessage() { return message; }
    public Instant getDate() { return date; }
    public String getStatus() { return status; }
}
