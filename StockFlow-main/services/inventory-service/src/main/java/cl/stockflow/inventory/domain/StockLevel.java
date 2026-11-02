package cl.stockflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

// Existencias actuales de un producto. Este servicio es su único dueño.
@Entity
@Table(name = "stock_levels")
public class StockLevel {

    @Id
    private UUID productId;

    @Column(nullable = false)
    private int currentStock;

    protected StockLevel() { }

    public StockLevel(UUID productId, int currentStock) {
        this.productId = productId;
        this.currentStock = currentStock;
    }

    public UUID getProductId() { return productId; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
}
