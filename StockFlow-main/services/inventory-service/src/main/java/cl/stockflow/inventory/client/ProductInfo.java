package cl.stockflow.inventory.client;

import java.util.UUID;

public record ProductInfo(UUID id, String code, String name, int minimumStock, boolean active) { }
