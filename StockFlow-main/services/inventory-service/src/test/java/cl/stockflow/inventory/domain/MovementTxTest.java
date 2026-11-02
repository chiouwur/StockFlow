package cl.stockflow.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.stockflow.inventory.api.ApiException;
import cl.stockflow.inventory.api.MovementRequest;
import cl.stockflow.inventory.api.MovementResponse;
import cl.stockflow.inventory.events.StockMovementRegistered;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

// Pruebas unitarias de las reglas de stock. No usan base de datos ni red.
class MovementTxTest {

    private StockLevelRepository stock;
    private MovementRepository movements;
    private ApplicationEventPublisher events;
    private MovementTx tx;
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stock = mock(StockLevelRepository.class);
        movements = mock(MovementRepository.class);
        events = mock(ApplicationEventPublisher.class);
        when(movements.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));
        tx = new MovementTx(stock, movements, events);
    }

    @Test
    void entryIncreasesStockAndPublishesEvent() {
        StockLevel level = new StockLevel(productId, 5);
        when(stock.findForUpdate(productId)).thenReturn(Optional.of(level));

        MovementResponse r = tx.apply(new MovementRequest(productId, MovementType.ENTRY, 10), 3, "u1");

        assertEquals(15, level.getCurrentStock());
        assertEquals(MovementType.ENTRY, r.type());
        ArgumentCaptor<StockMovementRegistered> captor = ArgumentCaptor.forClass(StockMovementRegistered.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(15, captor.getValue().stockAfter());
        assertEquals(3, captor.getValue().minimumStock());
        assertEquals("ENTRY", captor.getValue().movementType());
    }

    @Test
    void exitBeyondAvailableStockIsRejected() {
        StockLevel level = new StockLevel(productId, 2);
        when(stock.findForUpdate(productId)).thenReturn(Optional.of(level));

        ApiException e = assertThrows(ApiException.class,
                () -> tx.apply(new MovementRequest(productId, MovementType.EXIT, 5), 1, "u1"));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(2, level.getCurrentStock());
        verify(movements, never()).save(any(Movement.class));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void firstMovementCreatesTheStockRow() {
        when(stock.findForUpdate(productId)).thenReturn(Optional.empty());
        when(stock.saveAndFlush(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        tx.apply(new MovementRequest(productId, MovementType.ENTRY, 4), 1, "u1");

        verify(stock).saveAndFlush(any(StockLevel.class));
        ArgumentCaptor<StockMovementRegistered> captor = ArgumentCaptor.forClass(StockMovementRegistered.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(4, captor.getValue().stockAfter());
    }
}
