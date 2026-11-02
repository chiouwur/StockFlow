package cl.stockflow.notification.events;

import cl.stockflow.notification.config.RabbitConfig;
import cl.stockflow.notification.domain.AlertService;
import cl.stockflow.notification.domain.InvalidEventException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

/**
 * Consumidor de la cola notification.stock-movements con confirmación MANUAL (acknowledge-mode: manual).
 * Cada mensaje termina siempre en una de estas decisiones explícitas:
 *
 * <pre>
 * Situación                                   Decisión
 * ------------------------------------------  ---------------------------------------------------
 * Procesado correctamente                     ACK
 * Evento ya procesado (duplicado)             ACK (se ignora, no se repite la alerta)
 * JSON ilegible o evento inválido             NACK sin reencolar -> DLQ de inmediato (no recuperable)
 * Falla transitoria (base de datos, etc.)     reintentos con espera; si se agotan: NACK -> DLQ
 * Error inesperado                            NACK sin reencolar -> DLQ (no se reintenta a ciegas)
 * Aplicación detenida durante la espera       NACK con reencolado (el mensaje no se pierde)
 * </pre>
 *
 * Todo mensaje que va a la DLQ se registra en el log y en la tabla processing_errors.
 */
@Component
class StockMovementListener {

    private static final Logger log = LoggerFactory.getLogger(StockMovementListener.class);

    enum Outcome { OK, DUPLICATE, NON_RECOVERABLE, RETRIES_EXHAUSTED, UNEXPECTED, INTERRUPTED }

    private record Result(Outcome outcome, String reason) { }

    private final ObjectMapper mapper;
    private final AlertService alerts;
    private final DeadLetterRecorder recorder;
    private final int maxAttempts;
    private final long retryDelayMs;

    StockMovementListener(ObjectMapper mapper, AlertService alerts, DeadLetterRecorder recorder,
                          @Value("${app.messaging.max-attempts:3}") int maxAttempts,
                          @Value("${app.messaging.retry-delay-ms:1000}") long retryDelayMs) {
        this.mapper = mapper;
        this.alerts = alerts;
        this.recorder = recorder;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        StockMovementEvent event;
        try {
            event = mapper.readValue(message.getBody(), StockMovementEvent.class);
        } catch (IOException e) {
            rejectToDeadLetter(channel, tag, payload,
                    new Result(Outcome.NON_RECOVERABLE, "JSON ilegible: " + e.getMessage()));
            return;
        }

        Result result = handle(event);
        switch (result.outcome()) {
            case OK -> {
                channel.basicAck(tag, false);
                log.info("ACK eventId={}", event.eventId());
            }
            case DUPLICATE -> {
                channel.basicAck(tag, false);
                log.info("ACK (duplicado ignorado) eventId={}", event.eventId());
            }
            case INTERRUPTED -> {
                channel.basicNack(tag, false, true);
                log.warn("NACK con reencolado eventId={}: la aplicación se está deteniendo", event.eventId());
            }
            default -> rejectToDeadLetter(channel, tag, payload, result);
        }
    }

    // Decide qué errores se reintentan y cuáles no.
    private Result handle(StockMovementEvent event) {
        for (int attempt = 1; ; attempt++) {
            try {
                alerts.process(event);
                return new Result(Outcome.OK, "");
            } catch (InvalidEventException e) {
                return new Result(Outcome.NON_RECOVERABLE, e.getMessage());
            } catch (DataIntegrityViolationException e) {
                // La restricción única de eventId indica que otro intento ya guardó esta alerta.
                return new Result(Outcome.DUPLICATE, "evento ya procesado");
            } catch (DataAccessException | TransactionException e) {
                if (attempt >= maxAttempts) {
                    return new Result(Outcome.RETRIES_EXHAUSTED, "falló tras " + attempt + " intentos: " + e.getMessage());
                }
                log.warn("Falla transitoria (intento {} de {}): {}. Se reintenta.", attempt, maxAttempts, e.getMessage());
                try {
                    Thread.sleep(retryDelayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new Result(Outcome.INTERRUPTED, "interrumpido");
                }
            } catch (Exception e) {
                log.error("Error inesperado procesando el mensaje", e);
                return new Result(Outcome.UNEXPECTED, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    // NACK sin reencolar: RabbitMQ lo envía al exchange de mensajes fallidos y llega a la DLQ.
    private void rejectToDeadLetter(Channel channel, long tag, String payload, Result result) throws IOException {
        log.error("Mensaje enviado a la DLQ {} [{}]: {} | payload={}",
                RabbitConfig.DLQ, result.outcome(), result.reason(), payload);
        recorder.record(payload, result.outcome() + ": " + result.reason());
        channel.basicNack(tag, false, false);
    }
}
