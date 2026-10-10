package com.legacybridge.legacy.mock;

import com.legacybridge.legacy.jaxb.AuthorizeOperationRequest;
import com.legacybridge.legacy.jaxb.AuthorizeOperationResponse;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simula el sistema legacy real. Corre embebido en esta misma app (ver
 * LegacySoapConfig) para que el proyecto se pueda ejecutar y demostrar
 * sin depender de un sistema externo, pero se le habla por HTTP+SOAP/XML
 * real (no una llamada Java en memoria): el gateway le pega a
 * http://localhost:8080/ws/legacy igual que le pegaria a un sistema
 * legacy de verdad.
 *
 * Regla de negocio simulada para poder demostrar el escenario de error
 * sin depender de datos aleatorios: un monto mayor a 100000 excede el
 * limite que "el legacy" esta dispuesto a autorizar.
 */
@Endpoint
public class MockLegacyEndpoint {

    private static final String NAMESPACE_URI = "http://legacybridge.com/legacy/operation";
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("100000");

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "authorizeOperationRequest")
    @ResponsePayload
    public AuthorizeOperationResponse authorize(@RequestPayload AuthorizeOperationRequest request) {
        AuthorizeOperationResponse response = new AuthorizeOperationResponse();

        if (request.getAmount() != null && request.getAmount().compareTo(LIMITE_MAXIMO) > 0) {
            response.setStatus("ERROR");
            response.setMessage("El monto excede el limite permitido por el sistema legacy");
            return response;
        }

        response.setStatus("OK");
        response.setLegacyOperationId("LEGACY-" + UUID.randomUUID());
        response.setMessage("Operacion autorizada por el sistema legacy");
        return response;
    }
}
