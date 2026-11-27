package com.joyeriaEcommerce.AureaTPO.infraestructura.presentacion;

import com.joyeriaEcommerce.AureaTPO.infraestructura.errores.RecursoNoEncontradoException;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.LimiteIAExcedidoException;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PagoRechazadoException;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PasarelaNoDisponibleException;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ProveedorNoDisponibleException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.CredencialesInvalidasException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.DemasiadosIntentosException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.EmailYaRegistradoException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de errores en formato Problem Details (RFC 9457, application/problem+json).
 * Conserva las propiedades de extensión {@code mensaje} y {@code errores} por compatibilidad.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String TYPE_BASE = "https://aurea.local/problemas/";

    @ExceptionHandler(PagoRechazadoException.class)
    ResponseEntity<ProblemDetail> pagoRechazado(PagoRechazadoException ex, HttpServletRequest request) {
        String detalle = "Pago rechazado: " + ex.getMessage()
                + " No se registró el pedido; el carrito se conserva.";
        return problem(HttpStatus.PAYMENT_REQUIRED, "pago-rechazado", "Pago rechazado", detalle, request, Map.of());
    }

    @ExceptionHandler(PasarelaNoDisponibleException.class)
    ResponseEntity<ProblemDetail> pasarelaNoDisponible(PasarelaNoDisponibleException ex, HttpServletRequest request) {
        return conReintento(problem(HttpStatus.SERVICE_UNAVAILABLE, "pasarela-no-disponible",
                "Pasarela de pago no disponible", ex.getMessage(), request, Map.of()), ex.getSegundosParaReintentar());
    }

    @ExceptionHandler(ProveedorNoDisponibleException.class)
    ResponseEntity<ProblemDetail> proveedorNoDisponible(ProveedorNoDisponibleException ex, HttpServletRequest request) {
        return conReintento(problem(HttpStatus.SERVICE_UNAVAILABLE, "proveedor-no-disponible",
                "Proveedor externo no disponible", ex.getMessage(), request, Map.of()), ex.getSegundosParaReintentar());
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    ResponseEntity<ProblemDetail> emailDuplicado(EmailYaRegistradoException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "email-duplicado", "Email ya registrado", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ResponseEntity<ProblemDetail> usuarioNoEncontrado(UsuarioNoEncontradoException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "usuario-no-encontrado", "Usuario no encontrado", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ProblemDetail> credencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "Credenciales inválidas", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException ex, HttpServletRequest request) {
        return conReintento(problem(HttpStatus.TOO_MANY_REQUESTS, "demasiados-intentos",
                "Demasiados intentos", ex.getMessage(), request, Map.of()), ex.getSegundosParaReintentar());
    }

    @ExceptionHandler(LimiteIAExcedidoException.class)
    ResponseEntity<ProblemDetail> limiteIA(LimiteIAExcedidoException ex, HttpServletRequest request) {
        return conReintento(problem(HttpStatus.TOO_MANY_REQUESTS, "limite-ia",
                "Demasiadas solicitudes a la IA", ex.getMessage(), request, Map.of()), ex.getSegundosParaReintentar());
    }

    /** Más específico que IllegalArgumentException: Spring elige este manejador para un recurso inexistente. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> solicitudInvalida(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "solicitud-invalida", "Solicitud inválida", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<ProblemDetail> accesoDenegado(SecurityException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return problem(HttpStatus.BAD_REQUEST, "error-validacion",
                "Los datos enviados no son válidos", "Los datos enviados no son validos", request, errores);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concurrencia(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "conflicto-concurrencia", "Conflicto de concurrencia",
                "El recurso fue modificado por otro usuario. Por favor, recargue la página.", request, Map.of());
    }

    /**
     * Última barrera: la base rechazó los datos (largo, formato o unicidad) aunque pasaron la validación.
     * Responde 400 sin mostrar el SQL ni el nombre de tablas o columnas.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> datosRechazados(DataIntegrityViolationException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "datos-invalidos", "Datos inválidos",
                "Los datos enviados no cumplen las restricciones de largo, formato o unicidad.", request, Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> conflicto(IllegalStateException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "conflicto-estado", "Conflicto de estado", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> autenticacion(AuthenticationException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "autenticacion-requerida",
                "Autenticación fallida", "Credenciales invalidas", request, Map.of());
    }

    /** Agrega Retry-After (RFC 9110) y la misma espera como propiedad del Problem Details. */
    private static ResponseEntity<ProblemDetail> conReintento(ResponseEntity<ProblemDetail> respuesta, long segundos) {
        if (segundos <= 0) {
            return respuesta;
        }
        ProblemDetail cuerpo = respuesta.getBody();
        if (cuerpo != null) {
            cuerpo.setProperty("reintentarEnSegundos", segundos);
        }
        return ResponseEntity.status(respuesta.getStatusCode())
                .headers(respuesta.getHeaders())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(segundos))
                .body(cuerpo);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String slug,
            String title,
            String detail,
            HttpServletRequest request,
            Map<String, String> errores) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + slug));
        pd.setTitle(title);
        if (request != null && request.getRequestURI() != null) {
            pd.setInstance(URI.create(request.getRequestURI()));
        }
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("mensaje", detail);
        pd.setProperty("errores", errores);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
