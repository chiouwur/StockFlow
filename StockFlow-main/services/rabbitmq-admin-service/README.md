# rabbitmq-admin-service

Administración técnica de RabbitMQ para StockFlow (Spring Boot 3.5, Java 17, Maven). Puerto 8084.
Crea y elimina colas, exchanges y bindings usando `AmqpAdmin` (protocolo AMQP). No usa base de datos.

## Seguridad
Todas las rutas exigen un JWT válido (firma, emisor, audiencia) **y el rol de aplicación `RabbitAdmin`**
(claim `roles` del token de Microsoft Entra). Un usuario autenticado sin ese rol recibe 403; sin token, 401.
Cada acción queda en el log con el identificador del usuario.
Los elementos del flujo de StockFlow (`inventory.events`, `inventory.events.dlx`, `notification.stock-movements`,
`notification.stock-movements.dlq`) y los nombres `amq.*` no se pueden eliminar (409).

## Endpoints (prefijo /api/admin/rabbit)
| Método | Ruta | Cuerpo / parámetros | Respuestas |
|---|---|---|---|
| POST | /queues | `{ "name", "durable"?, "deadLetterExchange"?, "deadLetterRoutingKey"? }` | 201, 400, 409 |
| DELETE | /queues/{name} | | 204, 404, 409 |
| POST | /exchanges | `{ "name", "type": "DIRECT"\|"TOPIC"\|"FANOUT"\|"HEADERS", "durable"? }` | 201, 400, 409 |
| DELETE | /exchanges/{name} | | 204, 404, 409 |
| POST | /bindings | `{ "exchange", "queue", "routingKey"? }` | 201, 400, 404 |
| DELETE | /bindings | `?exchange=&queue=&routingKey=` | 204, 404, 409 |

Errores: `{ "status": 409, "message": "..." }`. Si RabbitMQ no responde: 503.

## Variables de entorno
JWT_ISSUER, JWT_JWK_SET_URI, JWT_AUDIENCE, ADMIN_ROLE (opcional, por defecto RabbitAdmin),
RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD, CORS_ORIGINS (opcional).

## Pruebas
    .\mvnw test
