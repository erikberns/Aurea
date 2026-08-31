package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.DatosActualizacionPerfil;
import jakarta.validation.constraints.NotBlank;

public record ActualizarPerfilRequest(@NotBlank String nombre, @NotBlank String apellido) {

    public DatosActualizacionPerfil toDatos() {
        return new DatosActualizacionPerfil(nombre, apellido);
    }
}
