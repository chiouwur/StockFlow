package cl.stockflow.rabbitadmin.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Todas las respuestas de error tienen la forma { "status": 409, "message": "..." }
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorBody(int status, String message) { }

    private ResponseEntity<ErrorBody> body(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(new ErrorBody(s.value(), m));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        var f = e.getBindingResult().getFieldError();
        return body(HttpStatus.BAD_REQUEST, f == null ? "Datos inválidos" : "Campo " + f.getField() + " inválido");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorBody> badRequest(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Solicitud inválida");
    }

    @ExceptionHandler(AmqpConnectException.class)
    ResponseEntity<ErrorBody> rabbitDown(AmqpConnectException e) {
        log.error("RabbitMQ no disponible", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible");
    }

    @ExceptionHandler(AmqpException.class)
    ResponseEntity<ErrorBody> rabbitError(AmqpException e) {
        log.error("Error al comunicarse con RabbitMQ", e);
        return body(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación (revisa los parámetros)");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorBody> notFound(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Ruta no encontrada");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e) {
        log.error("Error no controlado", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
