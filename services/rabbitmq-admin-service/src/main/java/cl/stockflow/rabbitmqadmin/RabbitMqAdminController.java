package cl.stockflow.rabbitmqadmin;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rabbitmq")
public class RabbitMqAdminController {

    private final RabbitMqAdminService adminService;

    public RabbitMqAdminController(RabbitMqAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/queues")
    public List<Map<String, Object>> queues() {
        return adminService.queues();
    }

    @GetMapping("/exchanges")
    public List<Map<String, Object>> exchanges() {
        return adminService.exchanges();
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> bindings() {
        return adminService.bindings();
    }
}