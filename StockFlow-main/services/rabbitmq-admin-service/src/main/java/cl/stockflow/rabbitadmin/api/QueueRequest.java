package cl.stockflow.rabbitadmin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueueRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,200}$") String name,
        Boolean durable,
        @Size(max = 200) String deadLetterExchange,
        @Size(max = 200) String deadLetterRoutingKey) { }
