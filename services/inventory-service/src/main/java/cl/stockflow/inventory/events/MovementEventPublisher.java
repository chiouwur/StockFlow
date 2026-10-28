package cl.stockflow.inventory.events;

// En EA2 se agrega una implementación que publica en un exchange de RabbitMQ.
public interface MovementEventPublisher {
    void publish(StockMovementRegistered event);
}
