package com.legacybridge.common.exception;

/**
 * Se lanza cuando un recurso solicitado (por ejemplo, una Operation por id)
 * no existe. El GlobalExceptionHandler la traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
