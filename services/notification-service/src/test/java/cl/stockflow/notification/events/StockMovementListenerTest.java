package cl.stockflow.notification.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cl.stockflow.notification.domain.AlertService;
import cl.stockflow.notification.domain.InvalidEventException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

// Verifica la decisión (ACK / NACK / reintento) que toma el consumidor en cada situación.
class StockMovementListenerTest {

    private static final long TAG = 7L;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private AlertService alerts;
    private DeadLetterRecorder recorder;
    private Channel channel;
    private StockMovementListener listener;

    @BeforeEach
    void setUp() {
        alerts = mock(AlertService.class);
        recorder = mock(DeadLetterRecorder.class);
        channel = mock(Channel.class);
        listener = new StockMovementListener(mapper, alerts, recorder, 3, 1L);
    }

    private Message validMessage() throws Exception {
        StockMovementEvent e = new StockMovementEvent(UUID.randomUUID(), "StockMovementRegistered", UUID.randomUUID(),
                UUID.randomUUID(), "EXIT", 5, 3, 5, Instant.now());
        return message(mapper.writeValueAsBytes(e));
    }

    private Message message(byte[] body) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        return new Message(body, props);
    }

    @Test
    void successfulProcessingIsAcknowledged() throws Exception {
        listener.onMessage(validMessage(), channel);

        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void unreadableJsonGoesToDeadLetterWithoutProcessing() throws Exception {
        listener.onMessage(message("esto no es json".getBytes()), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(alerts, never()).process(any(StockMovementEvent.class));
        verify(recorder).record(anyString(), contains("NON_RECOVERABLE"));
    }

    @Test
    void invalidEventGoesToDeadLetterWithoutRetries() throws Exception {
        doThrow(new InvalidEventException("evento inválido")).when(alerts).process(any(StockMovementEvent.class));

        listener.onMessage(validMessage(), channel);

        verify(alerts, times(1)).process(any(StockMovementEvent.class));
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(recorder).record(anyString(), contains("NON_RECOVERABLE"));
    }

    @Test
    void transientFailureIsRetriedAndThenAcknowledged() throws Exception {
        doThrow(new DataAccessResourceFailureException("base de datos caída"))
                .doNothing()
                .when(alerts).process(any(StockMovementEvent.class));

        listener.onMessage(validMessage(), channel);

        verify(alerts, times(2)).process(any(StockMovementEvent.class));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void transientFailureGoesToDeadLetterWhenRetriesAreExhausted() throws Exception {
        doThrow(new DataAccessResourceFailureException("base de datos caída"))
                .when(alerts).process(any(StockMovementEvent.class));

        listener.onMessage(validMessage(), channel);

        verify(alerts, times(3)).process(any(StockMovementEvent.class));
        verify(channel).basicNack(TAG, false, false);
        verify(recorder).record(anyString(), contains("RETRIES_EXHAUSTED"));
    }

    @Test
    void duplicateEventIsAcknowledgedWithoutRetrying() throws Exception {
        doThrow(new DataIntegrityViolationException("eventId duplicado"))
                .when(alerts).process(any(StockMovementEvent.class));

        listener.onMessage(validMessage(), channel);

        verify(alerts, times(1)).process(any(StockMovementEvent.class));
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void unexpectedErrorGoesToDeadLetterWithoutRetries() throws Exception {
        doThrow(new IllegalStateException("bug")).when(alerts).process(any(StockMovementEvent.class));

        listener.onMessage(validMessage(), channel);

        verify(alerts, times(1)).process(any(StockMovementEvent.class));
        verify(channel).basicNack(TAG, false, false);
        verify(recorder).record(anyString(), contains("UNEXPECTED"));
    }
}
