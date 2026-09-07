package com.legacybridge.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-legacybridge-32-bytes-min";

    private final JwtService jwtService = new JwtService(SECRET, 3600000L);

    @Test
    void generateToken_yLuegoExtractUsername_debeDevolverElMismoUsername() {
        UserDetails userDetails = User.withUsername("user.demo")
                .password("irrelevante-para-este-test")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user.demo");
    }

    @Test
    void isTokenValid_debeSerTrueParaUnTokenReicenGenerado() {
        UserDetails userDetails = User.withUsername("user.demo")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_debeSerFalseParaUnTokenManipulado() {
        UserDetails userDetails = User.withUsername("user.demo")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(userDetails);
        String tokenManipulado = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(tokenManipulado, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_debeSerFalseSiElTokenFueEmitidoParaOtroUsuario() {
        UserDetails autorDelToken = User.withUsername("user.demo").password("x").authorities("ROLE_USER").build();
        UserDetails otroUsuario = User.withUsername("otro.usuario").password("x").authorities("ROLE_USER").build();

        String token = jwtService.generateToken(autorDelToken);

        assertThat(jwtService.isTokenValid(token, otroUsuario)).isFalse();
    }
}
