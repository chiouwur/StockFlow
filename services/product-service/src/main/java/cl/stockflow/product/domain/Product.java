package cl.stockflow.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

// El catálogo no guarda existencias: eso es responsabilidad de inventory-service.
@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int minimumStock;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() { }

    public Product(String code, String name, int minimumStock) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.minimumStock = minimumStock;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
