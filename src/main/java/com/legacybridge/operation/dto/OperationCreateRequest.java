package com.legacybridge.operation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Payload de entrada para crear una Operation. Nunca se expone la
 * entidad JPA directamente en el Controller.
 */
public record OperationCreateRequest(

        @NotBlank(message = "externalId es obligatorio")
        String externalId,

        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.01", message = "amount debe ser mayor que 0")
        BigDecimal amount,

        @NotBlank(message = "currency es obligatoria")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency debe ser un codigo ISO 4217 de 3 letras mayusculas (ej. PEN, USD)")
        String currency
) {
}
