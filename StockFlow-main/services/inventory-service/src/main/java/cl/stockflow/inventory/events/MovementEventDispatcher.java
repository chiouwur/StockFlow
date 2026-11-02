package cl.stockflow.inventory.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Publica el evento SOLO después de que el movimiento quedó guardado en la base de datos.
// Limitación conocida: si la publicación falla, el evento se pierde (se registra en el log).
// La solución robusta es el patrón transactional outbox, a evaluar en EA2.
@Component
class MovementEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MovementEventDispatcher.class);
    private final MovementEventPublisher publisher;

    MovementEventDispatcher(MovementEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onMovement(StockMovementRegistered event) {
        try {
            publisher.publish(event);
        } catch (Exception e) {
            log.error("Movimiento guardado, pero falló la publicación del evento {}", event.eventId(), e);
        }
    }
}
