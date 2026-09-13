package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

/** Perfil público sin contraseña ni dependencias de las entidades JPA. */
public record UsuarioDTO(Long id, String nombre, String apellido, String email, String rol) {
}
