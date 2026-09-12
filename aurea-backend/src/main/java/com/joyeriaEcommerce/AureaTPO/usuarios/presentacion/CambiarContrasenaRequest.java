package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.DatosCambioContrasena;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaRequest(
        @NotBlank String contrasenaActual,
        @NotBlank @Size(min = 8) String contrasenaNueva) {

    public DatosCambioContrasena toDatos() {
        return new DatosCambioContrasena(contrasenaActual, contrasenaNueva);
    }
}
