package cl.stockflow.rabbitadmin.domain;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class TestMessageService {

    private static final String EXCHANGE = "inventory.events";
    private static final String ROUTING_KEY = "inventory.movement.registered";

    private final RabbitTemplate rabbitTemplate;

    public TestMessageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String kind) {

        String payload = switch (kind) {
            case "INVALID_EVENT" -> "{}";
            case "UNREADABLE_JSON" -> "{";
            default -> throw new IllegalArgumentException(
                    "Tipo de mensaje de prueba no válido: " + kind
            );
        };

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);

        Message message = new Message(
                payload.getBytes(StandardCharsets.UTF_8),
                properties
        );

        rabbitTemplate.send(EXCHANGE, ROUTING_KEY, message);
    }
}