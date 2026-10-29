package cl.stockflow.notification.config;

import cl.stockflow.notification.domain.ProcessingError;
import cl.stockflow.notification.domain.ProcessingErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Topología: inventory.events (topic) -> notification.stock-movements
// Mensajes rechazados -> inventory.events.dlx (direct) -> notification.stock-movements.dlq
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

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

    // El tipo del mensaje se toma del parámetro del listener, no de la clase del productor.
    @Bean
    MessageConverter eventMessageConverter(ObjectMapper mapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    // Se ejecuta cuando se agotan los reintentos: registra el error (log y base de datos)
    // y rechaza el mensaje sin devolverlo a la cola, con lo que RabbitMQ lo envía a la DLQ.
    @Bean
    MessageRecoverer failedMessageRecoverer(ProcessingErrorRepository errors) {
        return (message, cause) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Throwable root = cause.getCause() != null ? cause.getCause() : cause;
            String reason = String.valueOf(root.getMessage());
            log.error("Mensaje no procesado tras los reintentos; se envía a la DLQ {}. causa={} payload={}", DLQ, reason, payload);
            try {
                errors.save(new ProcessingError(truncate(payload, 4000), truncate(reason, 1000)));
            } catch (Exception e) {
                log.error("No se pudo registrar el error de procesamiento en la base de datos", e);
            }
            throw new AmqpRejectAndDontRequeueException("Reintentos agotados", cause);
        };
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
