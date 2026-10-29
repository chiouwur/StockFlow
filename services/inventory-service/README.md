# inventory-service

Existencias y movimientos de StockFlow (Spring Boot 3.5, Java 17, Maven). Puerto 8082.
Es el único dueño del stock. Consulta a product-service para saber si un producto existe y está activo.

## Endpoints
| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/movements | 201, 400, 404, 409, 502/503 |
| GET | /api/movements?productId= | 200 (últimos 200, más nuevos primero) |
| GET | /api/stock | 200 → [{productId, currentStock}] |
| GET | /api/stock/{productId} | 200 (0 si no tiene movimientos) |

Cuerpo de POST: `{ "productId": "uuid", "type": "ENTRY" | "EXIT", "quantity": 5 }`
Errores: `{ "status": 409, "message": "..." }`. Sin token válido: 401; sin el scope: 403.

## Reglas
- quantity entero > 0
- una salida no puede dejar el stock negativo (409)
- un producto desactivado no recibe movimientos (409)
- el stock se actualiza en el servidor, con bloqueo de fila, dentro de la misma transacción que guarda el movimiento

## Eventos (EA2)
Al guardar un movimiento se genera `StockMovementRegistered` (formato de la especificación).
Con `EVENTS_PUBLISHER=rabbit` se publica en el exchange `inventory.events` (tipo topic, durable) con la clave
`inventory.movement.registered`, como mensaje JSON persistente. Con `EVENTS_PUBLISHER=log` (por defecto) solo se escribe en el log.

### Qué pasa si el movimiento se guarda pero falla la publicación
El evento se publica solo DESPUÉS de confirmar la transacción (`@TransactionalEventListener(AFTER_COMMIT)`),
así nunca se anuncia un movimiento que no existe. La contrapartida: si RabbitMQ no responde en ese instante,
el movimiento queda guardado, el error se registra en el log (`falló la publicación del evento <eventId>`)
y el evento no se reenvía automáticamente, por lo que notification-service no genera la alerta.
La solución robusta es el patrón *transactional outbox* (guardar el evento en una tabla dentro de la misma
transacción y publicarlo desde allí con reintentos). No se implementa en el PMV por tiempo.

## Variables de entorno (modo normal)
DB_URL, DB_USER, DB_PASSWORD, PRODUCT_SERVICE_URL, JWT_ISSUER, JWT_JWK_SET_URI, JWT_AUDIENCE, EVENTS_PUBLISHER, RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD.

## Ejecutar en local (product-service debe estar corriendo en el puerto 8081)
    $env:SPRING_PROFILES_ACTIVE="local"
    .\mvnw spring-boot:run

## Probar
    $p = (Invoke-RestMethod http://localhost:8081/api/products)[0]
    $body = @{ productId = $p.id; type = 'ENTRY'; quantity = 10 } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/movements -ContentType 'application/json' -Body $body
    Invoke-RestMethod http://localhost:8082/api/stock
