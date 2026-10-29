package cl.stockflow.rabbitadmin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,200}$") String name,
        @NotNull ExchangeKind type,
        Boolean durable) { }
