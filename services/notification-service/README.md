# notification-service

Consumidor de eventos de inventario y alertas de stock bajo (Spring Boot 3.5, Java 17, Maven). Puerto 8083.

## Flujo
1. inventory-service publica `StockMovementRegistered` en el exchange `inventory.events` (clave `inventory.movement.registered`).
2. Este servicio consume la cola `notification.stock-movements`.
3. Si `stockAfter <= minimumStock` guarda una alerta (tabla `alerts`, una por evento).
4. Si el mensaje falla: 3 intentos (1 s, 2 s) y después se registra el error (log y tabla `processing_errors`)
   y se rechaza sin reencolar, con lo que RabbitMQ lo envía a la DLQ `notification.stock-movements.dlq`.

## Topología (se declara sola al arrancar)
- exchange `inventory.events` (topic, durable)
- cola `notification.stock-movements` (durable, con dead-letter hacia `inventory.events.dlx`)
- exchange `inventory.events.dlx` (direct) y cola `notification.stock-movements.dlq`

## Endpoint
| Método | Ruta | Respuestas |
|---|---|---|
| GET | /api/alerts | 200 (últimas 100, más nuevas primero) |

Sin token válido: 401; sin el scope: 403.

## Variables de entorno
DB_URL, DB_USER, DB_PASSWORD, JWT_ISSUER, JWT_JWK_SET_URI, JWT_AUDIENCE, RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD.

## Pruebas
    .\mvnw test
