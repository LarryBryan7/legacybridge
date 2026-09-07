package com.legacybridge.auth;

import com.jayway.jsonpath.JsonPath;
import com.legacybridge.auth.entity.Role;
import com.legacybridge.auth.entity.User;
import com.legacybridge.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el flujo real de extremo a extremo: login -> JWT -> endpoint
 * protegido, incluyendo autorizacion por rol. A diferencia de
 * OperationControllerValidationTest (slice de MVC con seguridad simulada
 * via @WithMockUser), aqui se carga el contexto completo -
 * SecurityConfig, JwtAuthenticationFilter, UserDetailsServiceImpl y JJWT
 * reales - contra una base H2 en memoria (ver src/test/resources).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    private static final String USER_PASSWORD = "User#12345";
    private static final String ADMIN_PASSWORD = "Admin#12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        userRepository.findByUsername("it.user")
                .orElseGet(() -> userRepository.save(
                        new User("it.user", passwordEncoder.encode(USER_PASSWORD), Role.ROLE_USER)));

        userRepository.findByUsername("it.admin")
                .orElseGet(() -> userRepository.save(
                        new User("it.admin", passwordEncoder.encode(ADMIN_PASSWORD), Role.ROLE_ADMIN)));
    }

    @Test
    void login_conCredencialesValidas_devuelve200YUnToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"it.user","password":"%s"}
                                """.formatted(USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"it.user","password":"clave-incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void operaciones_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/operations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operaciones_conTokenValido_devuelve200() throws Exception {
        String token = obtenerToken("it.user", USER_PASSWORD);

        mockMvc.perform(get("/api/v1/operations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void operaciones_conTokenInvalido_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/operations")
                        .header("Authorization", "Bearer esto-no-es-un-jwt-valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearOperacion_conRolUser_devuelve403() throws Exception {
        String token = obtenerToken("it.user", USER_PASSWORD);

        mockMvc.perform(post("/api/v1/operations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"IT-USER-001","amount":10.00,"currency":"PEN"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void crearOperacion_conRolAdmin_devuelve201() throws Exception {
        String token = obtenerToken("it.admin", ADMIN_PASSWORD);

        mockMvc.perform(post("/api/v1/operations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"IT-ADMIN-001","amount":10.00,"currency":"PEN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private String obtenerToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
