package cl.stockflow.inventory.api;

import cl.stockflow.inventory.domain.MovementService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final MovementService service;

    public StockController(MovementService service) {
        this.service = service;
    }

    @GetMapping
    public List<StockLevelResponse> all() {
        return service.allStock();
    }

    // Un producto sin movimientos tiene stock 0.
    @GetMapping("/{productId}")
    public StockLevelResponse one(@PathVariable UUID productId) {
        return service.stockOf(productId);
    }
}
