package cl.stockflow.inventory.domain;

import cl.stockflow.inventory.api.ApiException;
import cl.stockflow.inventory.api.MovementRequest;
import cl.stockflow.inventory.api.MovementResponse;
import cl.stockflow.inventory.events.StockMovementRegistered;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Parte transaccional: bloquea el stock, valida, actualiza y guarda el movimiento.
@Component
public class MovementTx {

    private final StockLevelRepository stock;
    private final MovementRepository movements;
    private final ApplicationEventPublisher events;

    public MovementTx(StockLevelRepository stock, MovementRepository movements, ApplicationEventPublisher events) {
        this.stock = stock;
        this.movements = movements;
        this.events = events;
    }

    @Transactional
    public MovementResponse apply(MovementRequest r, int minimumStock, String userId) {
        StockLevel level = stock.findForUpdate(r.productId())
                .orElseGet(() -> stock.saveAndFlush(new StockLevel(r.productId(), 0)));

        int delta = r.type() == MovementType.ENTRY ? r.quantity() : -r.quantity();
        long next = (long) level.getCurrentStock() + delta;
        if (next < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "La salida supera el stock disponible");
        }
        if (next > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cantidad demasiado grande");
        }
        level.setCurrentStock((int) next);

        Movement m = movements.save(new Movement(r.productId(), r.type(), r.quantity(), Instant.now(), userId));

        // Se entrega a RabbitMQ (o al log) solo después del commit
        events.publishEvent(new StockMovementRegistered(
                UUID.randomUUID(), "StockMovementRegistered", m.getId(), m.getProductId(),
                m.getType().name(), m.getQuantity(), level.getCurrentStock(), minimumStock, m.getDate()));
        return MovementResponse.from(m);
    }
}
