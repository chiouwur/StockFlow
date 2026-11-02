package cl.stockflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movements", indexes = @Index(columnList = "productId"))
public class Movement {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "occurred_at", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 100)
    private String userId;

    protected Movement() { }

    public Movement(UUID productId, MovementType type, int quantity, Instant date, String userId) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.date = date;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public Instant getDate() { return date; }
    public String getUserId() { return userId; }
}
