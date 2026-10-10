package com.legacybridge.legacy;

import com.legacybridge.legacy.jaxb.AuthorizeOperationRequest;
import com.legacybridge.legacy.jaxb.AuthorizeOperationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoapLegacyOperationGatewayTest {

    private static final String ENDPOINT_URL = "http://localhost:8080/ws/legacy";

    @Mock
    private WebServiceTemplate webServiceTemplate;

    @Test
    void authorize_conRespuestaExitosa_devuelveUnResultadoExitoso() {
        SoapLegacyOperationGateway gateway = new SoapLegacyOperationGateway(webServiceTemplate, ENDPOINT_URL);

        AuthorizeOperationResponse soapResponse = new AuthorizeOperationResponse();
        soapResponse.setStatus("OK");
        soapResponse.setLegacyOperationId("LEGACY-123");
        soapResponse.setMessage("Operacion autorizada por el sistema legacy");

        when(webServiceTemplate.marshalSendAndReceive(anyString(), any(AuthorizeOperationRequest.class))).thenReturn(soapResponse);

        LegacyOperationResult result = gateway.authorize("OP-1001", new BigDecimal("100.00"), "PEN");

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.legacyOperationId()).isEqualTo("LEGACY-123");
    }

    @Test
    void authorize_construyeElRequestSoapConLosDatosDeLaOperacion() {
        SoapLegacyOperationGateway gateway = new SoapLegacyOperationGateway(webServiceTemplate, ENDPOINT_URL);

        AuthorizeOperationResponse soapResponse = new AuthorizeOperationResponse();
        soapResponse.setStatus("OK");
        when(webServiceTemplate.marshalSendAndReceive(anyString(), any(AuthorizeOperationRequest.class))).thenReturn(soapResponse);

        gateway.authorize("OP-2002", new BigDecimal("250.50"), "USD");

        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(webServiceTemplate).marshalSendAndReceive(
                org.mockito.ArgumentMatchers.eq(ENDPOINT_URL), requestCaptor.capture());

        AuthorizeOperationRequest sentRequest = (AuthorizeOperationRequest) requestCaptor.getValue();
        assertThat(sentRequest.getExternalId()).isEqualTo("OP-2002");
        assertThat(sentRequest.getAmount()).isEqualByComparingTo("250.50");
        assertThat(sentRequest.getCurrency()).isEqualTo("USD");
    }

    @Test
    void authorize_conRespuestaDeError_devuelveUnResultadoNoExitoso() {
        SoapLegacyOperationGateway gateway = new SoapLegacyOperationGateway(webServiceTemplate, ENDPOINT_URL);

        AuthorizeOperationResponse soapResponse = new AuthorizeOperationResponse();
        soapResponse.setStatus("ERROR");
        soapResponse.setMessage("El monto excede el limite permitido por el sistema legacy");

        when(webServiceTemplate.marshalSendAndReceive(anyString(), any(AuthorizeOperationRequest.class))).thenReturn(soapResponse);

        LegacyOperationResult result = gateway.authorize("OP-3003", new BigDecimal("999999.00"), "PEN");

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("excede el limite");
    }

    @Test
    void authorize_conFalloDeComunicacion_lanzaLegacyIntegrationException() {
        SoapLegacyOperationGateway gateway = new SoapLegacyOperationGateway(webServiceTemplate, ENDPOINT_URL);

        when(webServiceTemplate.marshalSendAndReceive(anyString(), any(AuthorizeOperationRequest.class)))
                .thenThrow(new WebServiceIOException("Connection refused"));

        assertThatThrownBy(() -> gateway.authorize("OP-4004", BigDecimal.TEN, "PEN"))
                .isInstanceOf(LegacyIntegrationException.class);
    }
}
