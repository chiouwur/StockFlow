package cl.stockflow.notification.events;

import cl.stockflow.notification.config.RabbitConfig;
import cl.stockflow.notification.domain.AlertService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Consumidor de la cola notification.stock-movements.
// Si process() lanza una excepción, Spring reintenta; al agotar los intentos el mensaje va a la DLQ.
@Component
class StockMovementListener {

    private final AlertService alerts;

    StockMovementListener(AlertService alerts) {
        this.alerts = alerts;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    void onMessage(StockMovementEvent event) {
        alerts.process(event);
    }
}
