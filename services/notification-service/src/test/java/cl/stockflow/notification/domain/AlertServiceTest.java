package cl.stockflow.notification.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.stockflow.notification.events.StockMovementEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    private AlertRepository alerts;
    private AlertService service;

    @BeforeEach
    void setUp() {
        alerts = mock(AlertRepository.class);
        service = new AlertService(alerts);
    }

    private StockMovementEvent event(int stockAfter, int minimumStock) {
        return new StockMovementEvent(UUID.randomUUID(), "StockMovementRegistered", UUID.randomUUID(),
                UUID.randomUUID(), "EXIT", 5, stockAfter, minimumStock, Instant.now());
    }

    @Test
    void stockBelowMinimumCreatesAlert() {
        service.process(event(3, 5));
        verify(alerts).save(any(Alert.class));
    }

    @Test
    void stockEqualToMinimumCreatesAlert() {
        service.process(event(5, 5));
        verify(alerts).save(any(Alert.class));
    }

    @Test
    void enoughStockDoesNotCreateAlert() {
        service.process(event(10, 5));
        verify(alerts, never()).save(any(Alert.class));
    }

    @Test
    void alreadyProcessedEventIsIgnored() {
        StockMovementEvent e = event(1, 5);
        when(alerts.existsByEventId(e.eventId())).thenReturn(true);
        service.process(e);
        verify(alerts, never()).save(any(Alert.class));
    }

    @Test
    void invalidEventIsRejectedSoItGoesToTheDeadLetterQueue() {
        assertThrows(InvalidEventException.class, () -> service.process(event(-1, 5)));
    }
}
