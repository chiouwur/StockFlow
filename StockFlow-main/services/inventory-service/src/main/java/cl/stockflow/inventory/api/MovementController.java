package cl.stockflow.inventory.api;

import cl.stockflow.inventory.config.CurrentUser;
import cl.stockflow.inventory.domain.MovementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService service;

    public MovementController(MovementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MovementResponse> register(@Valid @RequestBody MovementRequest request) {
        MovementResponse created = service.register(request, CurrentUser.id());
        return ResponseEntity.created(URI.create("/api/movements/" + created.id())).body(created);
    }

    @GetMapping
    public List<MovementResponse> list(@RequestParam(required = false) UUID productId) {
        return service.list(productId);
    }
}
