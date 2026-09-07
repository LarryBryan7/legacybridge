package com.legacybridge.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se ejecuta exactamente una vez por request (de ahi OncePerRequestFilter).
 * Su unico trabajo: si viene un "Authorization: Bearer <token>" valido,
 * poblar el SecurityContext para que el resto de la cadena (y los
 * @PreAuthorize de los controllers) sepan quien esta autenticado.
 *
 * Si no hay header, o el token es invalido/expirado, simplemente NO
 * autentica y deja continuar la cadena: es la regla de la
 * SecurityFilterChain (.anyRequest().authenticated()) la que despues
 * rechaza la request con 401 si hacia falta estar autenticado.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            String username = jwtService.extractUsername(token);

            boolean noHayAutenticacionTodavia = SecurityContextHolder.getContext().getAuthentication() == null;
            if (username != null && noHayAutenticacionTodavia) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token malformado, expirado o con firma invalida: no se
            // autentica. No se lanza la excepcion hacia arriba porque un
            // filtro no deberia decidir el codigo HTTP final; eso lo
            // resuelve la regla de autorizacion de la SecurityFilterChain.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
