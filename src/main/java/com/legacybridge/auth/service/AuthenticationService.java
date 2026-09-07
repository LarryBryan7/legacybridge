package com.legacybridge.auth.service;

import com.legacybridge.auth.dto.LoginRequest;
import com.legacybridge.auth.dto.LoginResponse;
import com.legacybridge.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Orquesta el login: delega la verificacion de credenciales al
 * AuthenticationManager de Spring Security (que a su vez usa
 * UserDetailsServiceImpl + PasswordEncoder) y, si es exitosa, pide el
 * JWT a JwtService. No valida contraseñas por su cuenta: esa es
 * exactamente la responsabilidad que Spring Security ya resuelve.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Si las credenciales son invalidas, authenticationManager.authenticate
     * lanza BadCredentialsException (o UsernameNotFoundException, envuelta
     * como BadCredentialsException por el DaoAuthenticationProvider) - no
     * se captura aqui a proposito: el GlobalExceptionHandler la traduce a
     * un 401 consistente con el resto de la API.
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return LoginResponse.bearer(token, jwtService.getExpirationSeconds());
    }
}
