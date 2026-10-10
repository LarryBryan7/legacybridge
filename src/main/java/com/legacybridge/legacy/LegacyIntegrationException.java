package com.legacybridge.legacy;

/**
 * Cubre cualquier falla al integrar con el sistema legacy: no se pudo
 * establecer comunicacion (red, timeout), o la respuesta SOAP no se pudo
 * interpretar. El GlobalExceptionHandler la traduce a 502 Bad Gateway:
 * LegacyBridge actua de gateway, y quien fallo fue el sistema upstream,
 * no un error de esta API.
 */
public class LegacyIntegrationException extends RuntimeException {

    public LegacyIntegrationException(String message) {
        super(message);
    }

    public LegacyIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
