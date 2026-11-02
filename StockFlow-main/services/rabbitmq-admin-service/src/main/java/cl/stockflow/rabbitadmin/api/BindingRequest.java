package cl.stockflow.rabbitadmin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// routingKey puede ir vacía (por ejemplo en exchanges de tipo FANOUT).
public record BindingRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,200}$") String exchange,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,200}$") String queue,
        @Size(max = 255) String routingKey) { }
