package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de autenticación JWT (Bearer en header Authorization o cookie HttpOnly AUREA_JWT).
 * Valida firma, expiración y revocación en cada request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_COOKIE_NAME = "AUREA_JWT";
    private static final String BEARER_PREFIX = "Bearer ";

    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(IJwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            Optional<String> usernameOpt = jwtService.validateAndExtractUsername(token);
            if (usernameOpt.isEmpty()) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT inválido, expirado o revocado");
                return;
            }
            if (!autenticarEnContexto(usernameOpt.get(), request)) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario del token no encontrado");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing == null || !existing.isAuthenticated() || existing instanceof AnonymousAuthenticationToken) {
            String cookieToken = extractTokenFromCookie(request);
            if (cookieToken != null) {
                jwtService.validateAndExtractUsername(cookieToken)
                        .ifPresent(username -> autenticarEnContexto(username, request));
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean autenticarEnContexto(String username, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            return true;
        } catch (UsernameNotFoundException ex) {
            return false;
        }
    }

    public static String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

