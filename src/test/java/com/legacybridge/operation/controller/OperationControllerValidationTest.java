package com.legacybridge.operation.controller;

import com.legacybridge.auth.security.JwtService;
import com.legacybridge.operation.service.OperationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el manejo global de excepciones responde con 400 y un
 * cuerpo de error consistente cuando la validacion de Bean Validation falla.
 *
 * Con spring-boot-starter-security en el classpath, @WebMvcTest exige
 * autenticacion por defecto (usa la configuracion de seguridad por
 * defecto de Spring Boot, no importa nuestro SecurityConfig). @WithMockUser
 * simula un usuario ya autenticado para poder probar SOLO la validacion,
 * sin mezclarla con el comportamiento de auth (eso se prueba aparte en
 * AuthenticationIntegrationTest).
 */
@WebMvcTest(OperationController.class)
@WithMockUser(authorities = "ROLE_ADMIN")
class OperationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationService operationService;

    // JwtAuthenticationFilter es un @Component (Filter): @WebMvcTest lo
    // detecta y lo registra igual que a un Controller, asi que sus
    // dependencias deben resolverse aunque en este test no nos interese
    // su comportamiento (ya lo cubre AuthenticationIntegrationTest).
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void create_deberiaRetornar400CuandoFaltaExternalId() throws Exception {
        String bodyConExternalIdFaltante = """
                {
                  "amount": 2500.00,
                  "currency": "PEN"
                }
                """;

        mockMvc.perform(post("/api/v1/operations")
                        .with(csrf()) // sin SecurityConfig propio, el slice usa la seguridad
                                      // por defecto de Spring Boot, que si exige CSRF en POST
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConAmountInvalido))
                .andExpect(status().isBadRequest());
    }
}
