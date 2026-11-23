package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final AuthenticationProvider authenticationProvider;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final IJwtService jwtService;

        public SecurityConfig(
                        AuthenticationProvider authenticationProvider,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        IJwtService jwtService) {
                this.authenticationProvider = authenticationProvider;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtService = jwtService;
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers(req -> {
                                                        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
                                                        return auth != null && auth.startsWith("Bearer ");
                                                })
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/csrf", "/api/usuarios/sesion", "/api/usuarios",
                                                                "/api/usuarios/autenticar", "/actuator/health",
                                                                "/actuator/info")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/productos", "/api/productos/**")
                                                .permitAll()
                                                .requestMatchers("/api/carrito/checkout").authenticated()
                                                .requestMatchers("/api/carrito", "/api/carrito/items",
                                                                "/api/carrito/items/*")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(errors -> errors
                                                .authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .logout(logout -> logout
                                                .logoutUrl("/api/usuarios/salir")
                                                .addLogoutHandler((req, res, auth) -> {
                                                        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
                                                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                                                jwtService.revokeToken(authHeader.substring(7).trim());
                                                        }
                                                        String cookieToken = JwtAuthenticationFilter.extractTokenFromCookie(req);
                                                        if (cookieToken != null) {
                                                                jwtService.revokeToken(cookieToken);
                                                        }
                                                })
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN", JwtAuthenticationFilter.JWT_COOKIE_NAME)
                                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)))
                                .build();
        }

        @Bean
        UrlBasedCorsConfigurationSource corsConfigurationSource(
                        @Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(allowedOrigin));
                configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS", "PUT", "DELETE"));
                configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN",
                                "Idempotency-Key", "If-None-Match", "If-Match"));
                configuration.setExposedHeaders(List.of("Authorization", "ETag", "Idempotency-Replayed"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }
}
