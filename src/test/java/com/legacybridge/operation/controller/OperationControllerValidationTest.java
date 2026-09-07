package com.legacybridge.operation.controller;

import com.legacybridge.operation.service.OperationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el manejo global de excepciones responde con 400 y un
 * cuerpo de error consistente cuando la validacion de Bean Validation falla.
 */
@WebMvcTest(OperationController.class)
class OperationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationService operationService;

    @Test
    void create_deberiaRetornar400CuandoFaltaExternalId() throws Exception {
        String bodyConExternalIdFaltante = """
                {
                  "amount": 2500.00,
                  "currency": "PEN"
                }
                """;

        mockMvc.perform(post("/api/v1/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConExternalIdFaltante))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void create_deberiaRetornar400CuandoAmountEsCero() throws Exception {
        String bodyConAmountInvalido = """
                {
                  "externalId": "OP-1001",
                  "amount": 0,
                  "currency": "PEN"
                }
                """;

        mockMvc.perform(post("/api/v1/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConAmountInvalido))
                .andExpect(status().isBadRequest());
    }
}
