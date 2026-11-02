package cl.stockflow.notification.domain;

// Evento con datos inválidos: reintentar no sirve, el mensaje debe ir directo a la DLQ.
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
