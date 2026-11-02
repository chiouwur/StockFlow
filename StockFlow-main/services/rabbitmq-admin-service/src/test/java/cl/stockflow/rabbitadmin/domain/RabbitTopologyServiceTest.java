package cl.stockflow.rabbitadmin.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.stockflow.rabbitadmin.api.ApiException;
import cl.stockflow.rabbitadmin.api.BindingRequest;
import cl.stockflow.rabbitadmin.api.QueueRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.http.HttpStatus;

class RabbitTopologyServiceTest {

    private AmqpAdmin admin;
    private RabbitTopologyService service;

    @BeforeEach
    void setUp() {
        admin = mock(AmqpAdmin.class);
        service = new RabbitTopologyService(admin, mock(RabbitTemplate.class));
    }

    @Test
    void createQueueDeclaresItWhenItDoesNotExist() {
        when(admin.getQueueInfo("demo.cola")).thenReturn(null);

        service.createQueue(new QueueRequest("demo.cola", null, null, null));

        verify(admin).declareQueue(any(Queue.class));
    }

    @Test
    void createQueueRejectsExistingQueue() {
        when(admin.getQueueInfo("demo.cola")).thenReturn(mock(QueueInformation.class));

        ApiException e = assertThrows(ApiException.class,
                () -> service.createQueue(new QueueRequest("demo.cola", true, null, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(admin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void createQueueRejectsReservedNames() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.createQueue(new QueueRequest("amq.algo", true, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void deleteQueueRejectsStockFlowQueues() {
        ApiException e = assertThrows(ApiException.class, () -> service.deleteQueue("notification.stock-movements"));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(admin, never()).deleteQueue(anyString());
    }

    @Test
    void deleteQueueReturnsNotFoundWhenMissing() {
        when(admin.getQueueInfo("no.existe")).thenReturn(null);

        ApiException e = assertThrows(ApiException.class, () -> service.deleteQueue("no.existe"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void createBindingFailsWhenTheExchangeDoesNotExist() {
        // el RabbitTemplate simulado no confirma el exchange, así que se considera inexistente
        ApiException e = assertThrows(ApiException.class,
                () -> service.createBinding(new BindingRequest("demo.exchange", "demo.cola", "demo.#")));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
