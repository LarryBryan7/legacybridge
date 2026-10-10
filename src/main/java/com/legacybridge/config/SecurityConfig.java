package com.legacybridge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacybridge.auth.security.JwtAuthenticationFilter;
import com.legacybridge.common.exception.ApiError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Punto unico donde se define la politica de seguridad de la API. Todo lo
 * demas (filtro JWT, UserDetailsService, PasswordEncoder) son piezas que
 * esta clase ensambla, no logica de negocio propia.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en los controllers
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            // Mock Legacy System (Fase 3): simula un sistema externo. El JWT
            // de LegacyBridge protege NUESTRA API, no al legacy simulado -
            // son dos fronteras de seguridad distintas (ver README).
            "/ws/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // API stateless basada en JWT: no hay sesion de servidor que
                // proteger con un token CSRF (no se usan cookies de sesion).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Sin esto, Spring Security responde 403 (Http403ForbiddenEntryPoint,
                // su valor por defecto cuando no hay formLogin/httpBasic) incluso
                // para requests sin autenticar. Se fuerza aqui explicitamente el
                // 401 "de verdad" para peticiones no autenticadas, y se mantiene
                // el mismo formato ApiError que usa el resto de la API (estos dos
                // casos ocurren en la SecurityFilterChain, ANTES del
                // DispatcherServlet, asi que GlobalExceptionHandler no los alcanza).
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeApiError(
                response, HttpStatus.UNAUTHORIZED, "No autenticado", request.getRequestURI());
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeApiError(
                response, HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion", request.getRequestURI());
    }

    private void writeApiError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String message,
            String path
    ) throws java.io.IOException {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, path);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Conecta explicitamente UserDetailsService + PasswordEncoder. Sin este
     * bean, Spring Boot generaria un usuario en memoria por defecto (el que
     * imprime una contraseña aleatoria en los logs) en vez de usar nuestros
     * usuarios de la base de datos.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
