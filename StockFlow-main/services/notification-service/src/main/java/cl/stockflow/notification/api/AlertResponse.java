package cl.stockflow.notification.api;

import cl.stockflow.notification.domain.Alert;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(UUID id, UUID productId, String message, Instant date, String status) {
    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getProductId(), a.getMessage(), a.getDate(), a.getStatus());
    }
}
