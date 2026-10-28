package cl.stockflow.inventory.domain;

import cl.stockflow.inventory.api.ApiException;
import cl.stockflow.inventory.api.MovementRequest;
import cl.stockflow.inventory.api.MovementResponse;
import cl.stockflow.inventory.api.StockLevelResponse;
import cl.stockflow.inventory.client.ProductClient;
import cl.stockflow.inventory.client.ProductInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MovementService {

    private final ProductClient products;
    private final MovementTx tx;
    private final MovementRepository movements;
    private final StockLevelRepository stock;

    public MovementService(ProductClient products, MovementTx tx, MovementRepository movements, StockLevelRepository stock) {
        this.products = products;
        this.tx = tx;
        this.movements = movements;
        this.stock = stock;
    }

    // La consulta remota se hace ANTES de abrir la transacción para no mantener bloqueos mientras se espera.
    public MovementResponse register(MovementRequest r, String userId) {
        ProductInfo p = products.get(r.productId());
        if (!p.active()) {
            throw new ApiException(HttpStatus.CONFLICT, "El producto está desactivado");
        }
        return tx.apply(r, p.minimumStock(), userId);
    }

    public List<MovementResponse> list(UUID productId) {
        List<Movement> found = productId == null
                ? movements.findTop200ByOrderByDateDesc()
                : movements.findTop200ByProductIdOrderByDateDesc(productId);
        return found.stream().map(MovementResponse::from).toList();
    }

    public List<StockLevelResponse> allStock() {
        return stock.findAll().stream().map(s -> new StockLevelResponse(s.getProductId(), s.getCurrentStock())).toList();
    }

    public StockLevelResponse stockOf(UUID productId) {
        return stock.findById(productId)
                .map(s -> new StockLevelResponse(s.getProductId(), s.getCurrentStock()))
                .orElse(new StockLevelResponse(productId, 0));
    }
}
