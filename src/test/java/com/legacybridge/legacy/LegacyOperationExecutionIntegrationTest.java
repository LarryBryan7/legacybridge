package com.legacybridge.legacy;

import com.jayway.jsonpath.JsonPath;
import com.legacybridge.auth.entity.Role;
import com.legacybridge.auth.entity.User;
import com.legacybridge.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el flujo COMPLETO de la Fase 3 sobre HTTP real (no MockMvc):
 * login -> crear Operation -> POST /execute -> el gateway le habla por
 * SOAP/XML real al Mock Legacy System embebido (otro puerto/servlet de
 * la MISMA app) -> la Operation queda actualizada en la base de datos.
 *
 * webEnvironment = DEFINED_PORT (no RANDOM_PORT) para poder configurar
 * legacy.soap.endpoint-url con un puerto conocido de antemano en
 * src/test/resources/application.yml, sin la complejidad de resolver el
 * puerto real en tiempo de arranque.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class LegacyOperationExecutionIntegrationTest {

    private static final String ADMIN_PASSWORD = "Admin#12345";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdmin() {
        userRepository.findByUsername("legacy.it.admin")
                .orElseGet(() -> userRepository.save(
                        new User("legacy.it.admin", passwordEncoder.encode(ADMIN_PASSWORD), Role.ROLE_ADMIN)));
    }

    @Test
    void execute_flujoCompleto_ejecutaLaOperacionContraElMockLegacyRealPorSoap() {
        String token = login();

        Long operationId = crearOperacion(token, "IT-LEGACY-OK", new BigDecimal("500.00"));

        ResponseEntity<String> executeResponse = ejecutar(token, operationId);

        assertThat(executeResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(JsonPath.<String>read(executeResponse.getBody(), "$.status")).isEqualTo("COMPLETED");
        assertThat(JsonPath.<String>read(executeResponse.getBody(), "$.legacyOperationId")).startsWith("LEGACY-");
    }

    @Test
    void execute_conMontoQueElLegacyRechaza_dejaLaOperacionEnFailed() {
        String token = login();

        // Regla simulada en MockLegacyEndpoint: rechaza montos > 100000.
        Long operationId = crearOperacion(token, "IT-LEGACY-FAIL", new BigDecimal("999999.00"));

        ResponseEntity<String> executeResponse = ejecutar(token, operationId);

        assertThat(executeResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(JsonPath.<String>read(executeResponse.getBody(), "$.status")).isEqualTo("FAILED");
    }

    private String login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"username":"legacy.it.admin","password":"%s"}
                """.formatted(ADMIN_PASSWORD);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(body, headers), String.class);

        return JsonPath.read(response.getBody(), "$.token");
    }

    private Long crearOperacion(String token, String externalId, BigDecimal amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String body = """
                {"externalId":"%s","amount":%s,"currency":"PEN"}
                """.formatted(externalId, amount);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/operations", new HttpEntity<>(body, headers), String.class);

        Integer id = JsonPath.read(response.getBody(), "$.id");
        return id.longValue();
    }

    private ResponseEntity<String> ejecutar(String token, Long operationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return restTemplate.exchange(
                "/api/v1/operations/{id}/execute",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class,
                operationId);
    }
}
