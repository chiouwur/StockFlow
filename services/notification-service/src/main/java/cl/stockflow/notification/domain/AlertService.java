package cl.stockflow.notification.domain;

import cl.stockflow.notification.api.AlertResponse;
import cl.stockflow.notification.events.StockMovementEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alerts;

    public AlertService(AlertRepository alerts) {
        this.alerts = alerts;
    }

    // Regla de la especificación: si stockAfter <= minimumStock se registra una alerta.
    // Un evento inválido lanza InvalidEventException: el consumidor lo manda directo a la DLQ, sin reintentos.
    @Transactional
    public void process(StockMovementEvent e) {
        if (e.eventId() == null || e.productId() == null || e.stockAfter() < 0 || e.minimumStock() < 0) {
            throw new InvalidEventException("Evento inválido: faltan datos o hay valores negativos (eventId=" + e.eventId() + ")");
        }
        if (e.stockAfter() > e.minimumStock()) {
            log.info("Movimiento procesado sin alerta: productId={} stockAfter={} minimumStock={}", e.productId(), e.stockAfter(), e.minimumStock());
            return;
        }
        if (alerts.existsByEventId(e.eventId())) {
            log.info("Evento {} ya procesado; se ignora", e.eventId());
            return;
        }
        String message = "Stock bajo: quedan " + e.stockAfter() + " unidades (mínimo " + e.minimumStock() + ")";
        alerts.save(new Alert(e.productId(), e.eventId(), message, e.occurredAt() != null ? e.occurredAt() : java.time.Instant.now()));
        log.warn("ALERTA productId={} {}", e.productId(), message);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> latest() {
        return alerts.findTop100ByOrderByDateDesc().stream().map(AlertResponse::from).toList();
    }
}
