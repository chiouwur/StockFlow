package cl.stockflow.inventory.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Productor: publica StockMovementRegistered en el exchange inventory.events (mensaje persistente, JSON).
@Component
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "rabbit")
public class RabbitMovementEventPublisher implements MovementEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMovementEventPublisher.class);
    private final RabbitTemplate rabbit;

    public RabbitMovementEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(StockMovementRegistered event) {
        rabbit.convertAndSend(RabbitEventsConfig.EXCHANGE, RabbitEventsConfig.ROUTING_KEY, event);
        log.info("Evento publicado en RabbitMQ: eventId={} productId={} stockAfter={}",
                event.eventId(), event.productId(), event.stockAfter());
    }
}
