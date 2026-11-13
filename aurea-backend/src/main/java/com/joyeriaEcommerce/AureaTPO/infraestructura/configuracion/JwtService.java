package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Implementación RFC 7519 de JWT con firma HMAC-SHA256 (HS256), expiración corta (<= 15 min por defecto)
 * y lista de revocación en memoria para invalidación en logout (Criterios SEC-W03 y SEC-W04).
 */
@Service
public class JwtService implements IJwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern STRING_CLAIM = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_CLAIM = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(-?\\d+)");

    private final byte[] secretBytes;
    private final long expirationSeconds;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public JwtService(
            @Value("${app.jwt.secret:aurea-joyas-secret-key-2026-hs256-seguridad-tpo-uade}") String secret,
            @Value("${app.jwt.expiration-seconds:900}") long expirationSeconds) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String generateToken(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_CLIENTE");
        return generateToken(authentication.getName(), role);
    }

    @Override
    public String generateToken(String username, String role) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;
        String jti = UUID.randomUUID().toString();
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"rol\":\"%s\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\"}",
                escapeJson(username), escapeJson(role), now, exp, jti);

        String encodedHeader = base64UrlEncode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput);
        return signingInput + "." + signature;
    }

    @Override
    public Optional<String> validateAndExtractUsername(String token) {
        if (token == null || token.isBlank() || revokedTokens.contains(token)) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3 || parts[2].isBlank()) {
            return Optional.empty();
        }
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            // Rechazar alg "none" o cualquier algoritmo distinto de HS256 (SEC-W03)
            if (!"HS256".equals(extractStringClaim(headerJson, "alg"))) {
                return Optional.empty();
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Long exp = extractNumberClaim(payloadJson, "exp");
            if (exp == null || Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }

            String sub = extractStringClaim(payloadJson, "sub");
            if (sub == null || sub.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(sub);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            revokedTokens.add(token);
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token JWT", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String extractStringClaim(String json, String key) {
        Matcher matcher = STRING_CLAIM.matcher(json);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private static Long extractNumberClaim(String json, String key) {
        Matcher matcher = NUMBER_CLAIM.matcher(json);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) {
                return Long.parseLong(matcher.group(2));
            }
        }
        return null;
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

