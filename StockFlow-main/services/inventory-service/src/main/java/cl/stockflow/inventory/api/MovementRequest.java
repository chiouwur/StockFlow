package cl.stockflow.inventory.api;

import cl.stockflow.inventory.domain.MovementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record MovementRequest(
        @NotNull UUID productId,
        @NotNull MovementType type,
        @NotNull @Positive @Max(1_000_000) Integer quantity) { }
