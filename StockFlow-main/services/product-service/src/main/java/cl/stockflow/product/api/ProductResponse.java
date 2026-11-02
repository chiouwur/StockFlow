package cl.stockflow.product.api;

import cl.stockflow.product.domain.Product;
import java.util.UUID;

public record ProductResponse(UUID id, String code, String name, int minimumStock, boolean active) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getCode(), p.getName(), p.getMinimumStock(), p.isActive());
    }
}
