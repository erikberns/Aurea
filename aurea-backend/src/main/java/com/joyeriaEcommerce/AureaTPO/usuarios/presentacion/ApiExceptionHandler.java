package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.CredencialesInvalidasException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.EmailYaRegistradoException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.UsuarioNoEncontradoException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailYaRegistradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError emailDuplicado(EmailYaRegistradoException exception) {
        return new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError usuarioNoEncontrado(UsuarioNoEncontradoException exception) {
        return new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiError credencialesInvalidas(CredencialesInvalidasException exception) {
        return new ApiError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError solicitudInvalida(IllegalArgumentException exception) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiError accesoDenegado(SecurityException exception) {
        return new ApiError(Instant.now(), HttpStatus.FORBIDDEN.value(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError validacion(MethodArgumentNotValidException exception) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Los datos enviados no son validos",
                errores);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError concurrencia(org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
        return new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "El recurso fue modificado por otro usuario. Por favor, recargue la página.", Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError conflicto(IllegalStateException ex){return new ApiError(Instant.now(),409,ex.getMessage(),Map.of());}
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiError autenticacion(org.springframework.security.core.AuthenticationException ex){return new ApiError(Instant.now(),401,"Credenciales invalidas",Map.of());}
    record ApiError(Instant timestamp, int status, String mensaje, Map<String, String> errores) {
    }
}
