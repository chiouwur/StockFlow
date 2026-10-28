package cl.stockflow.inventory.api;

import cl.stockflow.inventory.domain.Movement;
import cl.stockflow.inventory.domain.MovementType;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(UUID id, UUID productId, MovementType type, int quantity, Instant date, String userId) {
    public static MovementResponse from(Movement m) {
        return new MovementResponse(m.getId(), m.getProductId(), m.getType(), m.getQuantity(), m.getDate(), m.getUserId());
    }
}
