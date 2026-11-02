package cl.stockflow.notification.events;

import cl.stockflow.notification.domain.ProcessingError;
import cl.stockflow.notification.domain.ProcessingErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Guarda en la base de datos cada mensaje que termina en la DLQ (además del log).
@Component
class DeadLetterRecorder {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterRecorder.class);
    private final ProcessingErrorRepository errors;

    DeadLetterRecorder(ProcessingErrorRepository errors) {
        this.errors = errors;
    }

    void record(String payload, String reason) {
        try {
            errors.save(new ProcessingError(truncate(payload, 4000), truncate(reason, 1000)));
        } catch (Exception e) {
            // Si la base de datos está caída no se puede registrar, pero el mensaje igual va a la DLQ.
            log.error("No se pudo registrar el error en la base de datos (el mensaje igual va a la DLQ)", e);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
