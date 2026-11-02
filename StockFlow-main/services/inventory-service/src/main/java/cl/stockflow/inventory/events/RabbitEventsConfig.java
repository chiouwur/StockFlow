package cl.stockflow.inventory.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Solo se activa con app.events.publisher=rabbit (variable EVENTS_PUBLISHER).
@Configuration
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "rabbit")
public class RabbitEventsConfig {

    public static final String EXCHANGE = "inventory.events";
    public static final String ROUTING_KEY = "inventory.movement.registered";

    @Bean
    TopicExchange inventoryEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    MessageConverter eventMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}
