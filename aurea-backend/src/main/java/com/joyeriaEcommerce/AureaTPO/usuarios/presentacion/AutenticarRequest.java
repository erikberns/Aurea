package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.Credenciales;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AutenticarRequest(
        @NotBlank @Email String email,
        @NotBlank String contrasena) {

    public Credenciales toCredenciales() {
        return new Credenciales(email, contrasena);
    }
}
