package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Contrato para emisión, validación y revocación de tokens JWT (HS256).
 */
public interface IJwtService {
    String generateToken(Authentication authentication);
    String generateToken(String username, String role);
    Optional<String> validateAndExtractUsername(String token);
    void revokeToken(String token);
}

