package com.legacybridge.legacy;

import com.legacybridge.legacy.jaxb.AuthorizeOperationRequest;
import com.legacybridge.legacy.jaxb.AuthorizeOperationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.math.BigDecimal;

/**
 * Unica clase del proyecto que sabe que la integracion con el legacy es
 * SOAP/XML sobre HTTP. Traduce entre el mundo "limpio" (primitivos,
 * LegacyOperationResult) y el mundo del protocolo (AuthorizeOperationRequest/
 * Response, generados/anotados con JAXB).
 */
@Component
public class SoapLegacyOperationGateway implements LegacyOperationGateway {

    private final WebServiceTemplate webServiceTemplate;
    private final String legacyEndpointUrl;

    public SoapLegacyOperationGateway(
            WebServiceTemplate legacyWebServiceTemplate,
            @Value("${legacy.soap.endpoint-url}") String legacyEndpointUrl
    ) {
        this.webServiceTemplate = legacyWebServiceTemplate;
        this.legacyEndpointUrl = legacyEndpointUrl;
    }

    @Override
    public LegacyOperationResult authorize(String externalId, BigDecimal amount, String currency) {
        AuthorizeOperationRequest request = new AuthorizeOperationRequest();
        request.setExternalId(externalId);
        request.setAmount(amount);
        request.setCurrency(currency);

        Object rawResponse;
        try {
            rawResponse = webServiceTemplate.marshalSendAndReceive(legacyEndpointUrl, request);
        } catch (WebServiceIOException e) {
            throw new LegacyIntegrationException("No se pudo establecer comunicacion con el sistema legacy", e);
        } catch (SoapFaultClientException e) {
            throw new LegacyIntegrationException("El sistema legacy respondio con un SOAP Fault", e);
        }

        if (!(rawResponse instanceof AuthorizeOperationResponse response)) {
            throw new LegacyIntegrationException(
                    "El sistema legacy devolvio una respuesta que no se pudo interpretar: " + rawResponse);
        }

        return new LegacyOperationResult(response.getLegacyOperationId(), response.getStatus(), response.getMessage());
    }
}
