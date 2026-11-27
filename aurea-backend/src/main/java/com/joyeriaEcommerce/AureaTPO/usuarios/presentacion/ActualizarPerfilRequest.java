package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.DatosActualizacionPerfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = RegistrarClienteRequest.SIN_CONTROL,
                message = "No se admiten caracteres de control") String nombre,
        @NotBlank @Size(max = 100) @Pattern(regexp = RegistrarClienteRequest.SIN_CONTROL,
                message = "No se admiten caracteres de control") String apellido) {

    public DatosActualizacionPerfil toDatos() {
        return new DatosActualizacionPerfil(nombre, apellido);
    }
}
