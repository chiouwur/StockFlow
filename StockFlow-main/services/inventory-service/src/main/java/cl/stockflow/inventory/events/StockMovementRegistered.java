package cl.stockflow.inventory.events;

import java.time.Instant;
import java.util.UUID;

// Mismo formato de evento definido en la especificación (sección E).
public record StockMovementRegistered(
        UUID eventId,
        String eventType,
        UUID movementId,
        UUID productId,
        String movementType,
        int quantity,
        int stockAfter,
        int minimumStock,
        Instant occurredAt) { }
