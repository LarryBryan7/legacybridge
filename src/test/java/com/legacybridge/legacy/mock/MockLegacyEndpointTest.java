package com.legacybridge.legacy.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.RequestCreators;
import org.springframework.ws.test.server.ResponseMatchers;
import org.springframework.xml.transform.StringSource;

import java.util.Map;

/**
 * Prueba MockLegacyEndpoint tal como lo veria un cliente SOAP real: se le
 * manda XML de verdad (marshalling incluido) y se valida el XML de
 * respuesta, usando el soporte de test de Spring-WS (analogo a MockMvc
 * pero para @Endpoint). No levanta un servidor HTTP real - eso lo cubre
 * LegacyOperationExecutionIntegrationTest.
 */
@SpringBootTest
class MockLegacyEndpointTest {

    private static final String NS = "http://legacybridge.com/legacy/operation";
    private static final Map<String, String> NAMESPACES = Map.of("op", NS);

    @Autowired
    private ApplicationContext applicationContext;

    private MockWebServiceClient client;

    @BeforeEach
    void setUp() {
        client = MockWebServiceClient.createClient(applicationContext);
    }

    @Test
    void authorize_conMontoDentroDelLimite_respondeOkConLegacyOperationId() throws Exception {
        StringSource request = new StringSource(
                "<authorizeOperationRequest xmlns='" + NS + "'>"
                        + "<externalId>OP-1001</externalId>"
                        + "<amount>100.00</amount>"
                        + "<currency>PEN</currency>"
                        + "</authorizeOperationRequest>");

        client.sendRequest(RequestCreators.withPayload(request))
                .andExpect(ResponseMatchers.noFault())
                .andExpect(ResponseMatchers.xpath("/op:authorizeOperationResponse/op:status/text()", NAMESPACES)
                        .evaluatesTo("OK"))
                .andExpect(ResponseMatchers.xpath("/op:authorizeOperationResponse/op:legacyOperationId/text()", NAMESPACES)
                        .exists());
    }

    @Test
    void authorize_conMontoQueExcedeElLimite_respondeError() throws Exception {
        StringSource request = new StringSource(
                "<authorizeOperationRequest xmlns='" + NS + "'>"
                        + "<externalId>OP-9999</externalId>"
                        + "<amount>500000.00</amount>"
                        + "<currency>PEN</currency>"
                        + "</authorizeOperationRequest>");

        client.sendRequest(RequestCreators.withPayload(request))
                .andExpect(ResponseMatchers.noFault())
                .andExpect(ResponseMatchers.xpath("/op:authorizeOperationResponse/op:status/text()", NAMESPACES)
                        .evaluatesTo("ERROR"));
    }
}
