package cl.stockflow.rabbitadmin.domain;

import cl.stockflow.rabbitadmin.api.ApiException;
import cl.stockflow.rabbitadmin.api.BindingRequest;
import cl.stockflow.rabbitadmin.api.ExchangeRequest;
import cl.stockflow.rabbitadmin.api.QueueRequest;
import cl.stockflow.rabbitadmin.config.CurrentUser;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RabbitTopologyService {

    private static final Logger log = LoggerFactory.getLogger(RabbitTopologyService.class);

    // Elementos de StockFlow que no se pueden borrar desde esta API: romperían el flujo de eventos.
    static final Set<String> PROTECTED = Set.of(
            "inventory.events", "inventory.events.dlx",
            "notification.stock-movements", "notification.stock-movements.dlq");

    private final AmqpAdmin admin;
    private final RabbitTemplate rabbit;

    public RabbitTopologyService(AmqpAdmin admin, RabbitTemplate rabbit) {
        this.admin = admin;
        this.rabbit = rabbit;
    }

    // ---------- colas ----------
    public void createQueue(QueueRequest r) {
        checkNotReserved(r.name());
        if (queueExists(r.name())) {
            throw new ApiException(HttpStatus.CONFLICT, "La cola ya existe");
        }
        boolean durable = r.durable() == null || r.durable();
        QueueBuilder b = durable ? QueueBuilder.durable(r.name()) : QueueBuilder.nonDurable(r.name());
        if (r.deadLetterExchange() != null && !r.deadLetterExchange().isBlank()) {
            b.deadLetterExchange(r.deadLetterExchange());
            if (r.deadLetterRoutingKey() != null && !r.deadLetterRoutingKey().isBlank()) {
                b.deadLetterRoutingKey(r.deadLetterRoutingKey());
            }
        }
        admin.declareQueue(b.build());
        log.info("ADMIN usuario={} creó la cola {}", CurrentUser.id(), r.name());
    }

    public void deleteQueue(String name) {
        checkNotProtected(name);
        if (!queueExists(name)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La cola no existe");
        }
        admin.deleteQueue(name);
        log.info("ADMIN usuario={} eliminó la cola {}", CurrentUser.id(), name);
    }

    // ---------- exchanges ----------
    public void createExchange(ExchangeRequest r) {
        checkNotReserved(r.name());
        if (exchangeExists(r.name())) {
            throw new ApiException(HttpStatus.CONFLICT, "El exchange ya existe");
        }
        boolean durable = r.durable() == null || r.durable();
        Exchange ex = switch (r.type()) {
            case DIRECT -> new DirectExchange(r.name(), durable, false);
            case TOPIC -> new TopicExchange(r.name(), durable, false);
            case FANOUT -> new FanoutExchange(r.name(), durable, false);
            case HEADERS -> new HeadersExchange(r.name(), durable, false);
        };
        admin.declareExchange(ex);
        log.info("ADMIN usuario={} creó el exchange {} ({})", CurrentUser.id(), r.name(), r.type());
    }

    public void deleteExchange(String name) {
        checkNotProtected(name);
        if (!exchangeExists(name)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El exchange no existe");
        }
        admin.deleteExchange(name);
        log.info("ADMIN usuario={} eliminó el exchange {}", CurrentUser.id(), name);
    }

    // ---------- bindings ----------
    public void createBinding(BindingRequest r) {
        if (!exchangeExists(r.exchange())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El exchange no existe");
        }
        if (!queueExists(r.queue())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La cola no existe");
        }
        admin.declareBinding(toBinding(r));
        log.info("ADMIN usuario={} creó el binding {} -> {} (clave '{}')", CurrentUser.id(), r.exchange(), r.queue(), key(r));
    }

    public void deleteBinding(BindingRequest r) {
        if (PROTECTED.contains(r.exchange()) || PROTECTED.contains(r.queue())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese binding pertenece al flujo de StockFlow y está protegido");
        }
        if (!exchangeExists(r.exchange())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El exchange no existe");
        }
        if (!queueExists(r.queue())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La cola no existe");
        }
        admin.removeBinding(toBinding(r));
        log.info("ADMIN usuario={} eliminó el binding {} -> {} (clave '{}')", CurrentUser.id(), r.exchange(), r.queue(), key(r));
    }

    // ---------- utilidades ----------
    private Binding toBinding(BindingRequest r) {
        return new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(), key(r), null);
    }

    private static String key(BindingRequest r) {
        return r.routingKey() == null ? "" : r.routingKey();
    }

    private boolean queueExists(String name) {
        return admin.getQueueInfo(name) != null;
    }

    private boolean exchangeExists(String name) {
        try {
            return Boolean.TRUE.equals(rabbit.execute(channel -> {
                channel.exchangeDeclarePassive(name);
                return true;
            }));
        } catch (Exception e) {
            return false;   // RabbitMQ responde 404 y cierra el canal cuando el exchange no existe
        }
    }

    private void checkNotReserved(String name) {
        if (name.startsWith("amq.")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los nombres que empiezan con amq. están reservados por RabbitMQ");
        }
    }

    private void checkNotProtected(String name) {
        if (PROTECTED.contains(name) || name.startsWith("amq.")) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese elemento pertenece al flujo de StockFlow y está protegido");
        }
    }
}
