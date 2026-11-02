package cl.stockflow.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.stockflow.inventory.api.ApiException;
import cl.stockflow.inventory.api.MovementRequest;
import cl.stockflow.inventory.api.MovementResponse;
import cl.stockflow.inventory.client.ProductClient;
import cl.stockflow.inventory.client.ProductInfo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MovementServiceTest {

    private ProductClient products;
    private MovementTx tx;
    private StockLevelRepository stock;
    private MovementService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductClient.class);
        tx = mock(MovementTx.class);
        stock = mock(StockLevelRepository.class);
        service = new MovementService(products, tx, mock(MovementRepository.class), stock);
    }

    @Test
    void inactiveProductIsRejectedWithoutTouchingStock() {
        UUID id = UUID.randomUUID();
        when(products.get(id)).thenReturn(new ProductInfo(id, "A", "Uno", 2, false));

        ApiException e = assertThrows(ApiException.class,
                () -> service.register(new MovementRequest(id, MovementType.ENTRY, 1), "u1"));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verifyNoInteractions(tx);
    }

    @Test
    void activeProductIsRegisteredWithItsMinimumStock() {
        UUID id = UUID.randomUUID();
        MovementRequest req = new MovementRequest(id, MovementType.ENTRY, 1);
        MovementResponse expected = new MovementResponse(UUID.randomUUID(), id, MovementType.ENTRY, 1, Instant.now(), "u1");
        when(products.get(id)).thenReturn(new ProductInfo(id, "A", "Uno", 2, true));
        when(tx.apply(any(MovementRequest.class), eq(2), eq("u1"))).thenReturn(expected);

        assertSame(expected, service.register(req, "u1"));
    }

    @Test
    void productWithoutMovementsHasZeroStock() {
        UUID id = UUID.randomUUID();
        when(stock.findById(id)).thenReturn(Optional.empty());

        assertEquals(0, service.stockOf(id).currentStock());
    }
}
