# notification-service

Consumidor de eventos de inventario y alertas de stock bajo (Spring Boot 3.5, Java 17, Maven). Puerto 8083.

## Flujo
1. inventory-service publica `StockMovementRegistered` en el exchange `inventory.events` (clave `inventory.movement.registered`).
2. Este servicio consume la cola `notification.stock-movements` con **confirmación manual** (ACK/NACK explícito).
3. Si `stockAfter <= minimumStock` guarda una alerta (tabla `alerts`, una por evento).

## Confirmación de mensajes y manejo de errores
| Situación | Decisión |
|---|---|
| Procesado correctamente | ACK |
| Evento ya procesado (duplicado) | ACK, se ignora |
| JSON ilegible o evento inválido (no recuperable) | NACK sin reencolar: va a la DLQ de inmediato, sin reintentos |
| Falla transitoria (por ejemplo, base de datos caída) | Reintentos con espera creciente (`app.messaging.max-attempts`, `retry-delay-ms`); si se agotan, NACK a la DLQ |
| Error inesperado | NACK sin reencolar: va a la DLQ, no se reintenta a ciegas |
| Aplicación detenida durante la espera | NACK con reencolado: el mensaje no se pierde |

Todo mensaje que llega a la DLQ se registra en el log (`Mensaje enviado a la DLQ ...`) y en la tabla `processing_errors`
con su contenido y el motivo (`NON_RECOVERABLE`, `RETRIES_EXHAUSTED`, `UNEXPECTED`).
Los mensajes de la DLQ quedan en RabbitMQ para revisarlos o reprocesarlos manualmente.

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
