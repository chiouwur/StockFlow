package cl.stockflow.notification.events;

import java.time.Instant;
import java.util.UUID;

// Mismo contrato que publica inventory-service (sección E de la especificación).
public record StockMovementEvent(
        UUID eventId,
        String eventType,
        UUID movementId,
        UUID productId,
        String movementType,
        int quantity,
        int stockAfter,
        int minimumStock,
        Instant occurredAt) { }
