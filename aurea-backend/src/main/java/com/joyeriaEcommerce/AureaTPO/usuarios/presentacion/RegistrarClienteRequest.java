package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.DatosRegistro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarClienteRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String contrasena) {

    public DatosRegistro toDatos() {
        return new DatosRegistro(nombre, apellido, email, contrasena);
    }
}
