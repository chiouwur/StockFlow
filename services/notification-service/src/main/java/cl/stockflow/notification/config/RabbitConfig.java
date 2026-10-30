package cl.stockflow.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Topología: inventory.events (topic) -> notification.stock-movements
// Mensajes rechazados (NACK sin reencolar) -> inventory.events.dlx (direct) -> notification.stock-movements.dlq
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "inventory.events";
    public static final String ROUTING_KEY = "inventory.movement.registered";
    public static final String QUEUE = "notification.stock-movements";
    public static final String DLX = "inventory.events.dlx";
    public static final String DLQ = "notification.stock-movements.dlq";

    @Bean
    TopicExchange inventoryEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue stockMovementsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding stockMovementsBinding() {
        return BindingBuilder.bind(stockMovementsQueue()).to(inventoryEventsExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }
}
