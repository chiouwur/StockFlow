package cl.stockflow.inventory.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Implementación provisional: escribe el evento en el log. Sirve hasta que exista RabbitMQ.
@Component
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "log", matchIfMissing = true)
public class LoggingMovementEventPublisher implements MovementEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingMovementEventPublisher.class);
    private final ObjectMapper mapper;

    public LoggingMovementEventPublisher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void publish(StockMovementRegistered event) {
        try {
            log.info("EVENTO {}", mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar el evento {}", event.eventId());
        }
    }
}
