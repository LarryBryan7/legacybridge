package com.legacybridge.auth.service;

import com.legacybridge.auth.dto.LoginRequest;
import com.legacybridge.auth.dto.LoginResponse;
import com.legacybridge.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Test
    void login_conCredencialesValidas_devuelveUnTokenJwt() {
        AuthenticationService authenticationService = new AuthenticationService(authenticationManager, jwtService);

        UserDetails userDetails = User.withUsername("user.demo").password("x").authorities("ROLE_USER").build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new TestingAuthenticationToken(userDetails, null, "ROLE_USER"));
        when(jwtService.generateToken(any())).thenReturn("token-simulado");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authenticationService.login(new LoginRequest("user.demo", "cualquier-clave"));

        assertThat(response.token()).isEqualTo("token-simulado");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void login_conCredencialesIncorrectas_propagaBadCredentialsException() {
        AuthenticationService authenticationService = new AuthenticationService(authenticationManager, jwtService);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user.demo", "clave-incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
