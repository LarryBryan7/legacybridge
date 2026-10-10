package com.legacybridge.operation.dto;

import com.legacybridge.operation.entity.Operation;
import com.legacybridge.operation.entity.OperationStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representacion publica de una Operation. Desacopla el contrato de la
 * API del modelo de persistencia.
 */
public record OperationResponse(
        Long id,
        String externalId,
        BigDecimal amount,
        String currency,
        OperationStatus status,
        String legacyOperationId,
        Instant createdAt,
        Instant updatedAt
) {

    public static OperationResponse fromEntity(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getExternalId(),
                operation.getAmount(),
                operation.getCurrency(),
                operation.getStatus(),
                operation.getLegacyOperationId(),
                operation.getCreatedAt(),
                operation.getUpdatedAt()
        );
    }
}
