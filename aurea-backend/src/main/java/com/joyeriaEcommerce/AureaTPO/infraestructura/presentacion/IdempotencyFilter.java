package com.joyeriaEcommerce.AureaTPO.infraestructura.presentacion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Filtro de idempotencia HTTP para peticiones {@code POST /api/**} que envían la cabecera {@code Idempotency-Key}.
 * Evita duplicar recursos (p. ej. órdenes o checkouts) ante reintentos por timeout o fallas de red.
 * Si la misma clave se reutiliza con un cuerpo diferente, responde {@code 409 Conflict} en formato Problem Details.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";
    private static final Duration TTL = Duration.ofHours(24);
    private static final int MAX_ENTRIES = 10_000;

    private final ObjectMapper objectMapper;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public IdempotencyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return true;
        }
        return request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isEmpty() || key.length() > 128) {
            escribirProblema(response, request, HttpStatus.BAD_REQUEST, "idempotency-key-invalida",
                    "Idempotency-Key inválida", "La cabecera Idempotency-Key debe tener entre 1 y 128 caracteres.");
            return;
        }

        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String bodyHash = sha256(bodyBytes);
        String scopeKey = construirClave(request, key);

        limpiarExpiradosSiExcede();

        Registro enCurso = Registro.enCurso(bodyHash);
        Registro previo = registros.putIfAbsent(scopeKey, enCurso);
        if (previo != null) {
            if (previo.expirado()) {
                registros.replace(scopeKey, previo, enCurso);
            } else if (!previo.bodyHash().equals(bodyHash)) {
                escribirProblema(response, request, HttpStatus.CONFLICT, "idempotency-key-conflicto",
                        "Conflicto de Idempotency-Key",
                        "La cabecera Idempotency-Key ya fue utilizada con una solicitud diferente.");
                return;
            } else if (previo.enProgreso()) {
                escribirProblema(response, request, HttpStatus.CONFLICT, "idempotency-en-curso",
                        "Solicitud idempotente en curso",
                        "Ya existe una solicitud en progreso con esta Idempotency-Key.");
                return;
            } else {
                response.setStatus(previo.status());
                if (previo.contentType() != null) {
                    response.setContentType(previo.contentType());
                }
                response.setHeader(REPLAYED_HEADER, "true");
                response.getOutputStream().write(previo.responseBody());
                return;
            }
        }

        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request, bodyBytes);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            int status = wrappedResponse.getStatus();
            if (status >= 200 && status < 300) {
                registros.put(scopeKey, Registro.completado(
                        bodyHash, status, wrappedResponse.getContentType(), wrappedResponse.getContentAsByteArray()));
            } else {
                registros.remove(scopeKey, enCurso);
            }
            wrappedResponse.copyBodyToResponse();
        } catch (Throwable ex) {
            registros.remove(scopeKey, enCurso);
            throw ex;
        }
    }

    private String construirClave(HttpServletRequest request, String key) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null && auth.isAuthenticated() && auth.getName() != null)
                ? auth.getName()
                : (request.getSession(false) != null ? "sid:" + request.getSession(false).getId() : "anon");
        return actor + "|" + request.getMethod() + "|" + request.getRequestURI() + "|" + key;
    }

    private void escribirProblema(HttpServletResponse response, HttpServletRequest request,
                                  HttpStatus status, String slug, String title, String detail) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://aurea.local/problemas/" + slug));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("mensaje", detail);
        pd.setProperty("errores", Map.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }

    private void limpiarExpiradosSiExcede() {
        if (registros.size() > MAX_ENTRIES) {
            registros.entrySet().removeIf(e -> e.getValue().expirado());
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Registro(String bodyHash, boolean enProgreso, int status, String contentType,
                            byte[] responseBody, Instant expiraEn) {
        static Registro enCurso(String bodyHash) {
            return new Registro(bodyHash, true, 0, null, new byte[0], Instant.now().plus(TTL));
        }

        static Registro completado(String bodyHash, int status, String contentType, byte[] responseBody) {
            return new Registro(bodyHash, false, status, contentType, responseBody, Instant.now().plus(TTL));
        }

        boolean expirado() {
            return Instant.now().isAfter(expiraEn);
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
