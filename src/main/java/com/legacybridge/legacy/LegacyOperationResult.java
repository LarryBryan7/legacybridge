package com.legacybridge.legacy;

/**
 * Resultado de pedirle al sistema legacy que autorice una operacion.
 * Es el tipo que cruza la frontera hacia OperationService: no tiene
 * absolutamente nada de SOAP/XML, para que el Service no necesite saber
 * como se llego a este resultado.
 */
public record LegacyOperationResult(
        String legacyOperationId,
        String status,
        String message
) {

    public boolean isSuccessful() {
        return "OK".equalsIgnoreCase(status);
    }
}
