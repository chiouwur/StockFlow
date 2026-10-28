package cl.stockflow.inventory.api;

import java.util.UUID;

public record StockLevelResponse(UUID productId, int currentStock) { }
