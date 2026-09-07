package com.legacybridge.auth.dto;

/**
 * expiresInSeconds se expone (en lugar de solo devolver el JWT) para que
 * el cliente sepa cuando debe volver a autenticarse, sin tener que
 * decodificar el token el mismo.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {

    public static LoginResponse bearer(String token, long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
