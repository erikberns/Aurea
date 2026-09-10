package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Rol;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;

public record UsuarioDTO(Long id, String nombre, String apellido, String email, Rol rol) {

    public static UsuarioDTO desde(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getFirstName(),
                usuario.getLastName(),
                usuario.getEmail(),
                usuario.getRole());
    }
}
