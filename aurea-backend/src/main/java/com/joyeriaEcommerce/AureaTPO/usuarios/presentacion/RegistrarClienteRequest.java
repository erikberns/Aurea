package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.DatosRegistro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarClienteRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = SIN_CONTROL, message = "No se admiten caracteres de control") String nombre,
        @NotBlank @Size(max = 100) @Pattern(regexp = SIN_CONTROL, message = "No se admiten caracteres de control") String apellido,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String contrasena) {

    /** Texto sin caracteres de control (\u0000, saltos de línea, etc.): PostgreSQL rechaza \u0000 en columnas de texto. */
    static final String SIN_CONTROL = "[^\\p{Cntrl}]*";

    public DatosRegistro toDatos() {
        return new DatosRegistro(nombre, apellido, email, contrasena);
    }
}
