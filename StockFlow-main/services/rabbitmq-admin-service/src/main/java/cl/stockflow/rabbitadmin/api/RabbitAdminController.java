package cl.stockflow.rabbitadmin.api;

import cl.stockflow.rabbitadmin.domain.RabbitTopologyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Solo usuarios con el rol RabbitAdmin pueden usar estas rutas (ver SecurityConfig).
@RestController
@RequestMapping("/api/admin/rabbit")
public class RabbitAdminController {

    private final RabbitTopologyService service;

    public RabbitAdminController(RabbitTopologyService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueRequest createQueue(@Valid @RequestBody QueueRequest request) {
        service.createQueue(request);
        return request;
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQueue(@PathVariable String name) {
        service.deleteQueue(name);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequest createExchange(@Valid @RequestBody ExchangeRequest request) {
        service.createExchange(request);
        return request;
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExchange(@PathVariable String name) {
        service.deleteExchange(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest createBinding(@Valid @RequestBody BindingRequest request) {
        service.createBinding(request);
        return request;
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBinding(@RequestParam String exchange, @RequestParam String queue,
                              @RequestParam(required = false, defaultValue = "") String routingKey) {
        service.deleteBinding(new BindingRequest(exchange, queue, routingKey));
    }
}
