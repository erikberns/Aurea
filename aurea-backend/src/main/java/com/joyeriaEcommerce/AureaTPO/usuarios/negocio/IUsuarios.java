package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

public interface IUsuarios {

    AuthResponse registrarCliente(DatosRegistro datos);

    AuthResponse autenticar(Credenciales credenciales);

    UsuarioDTO consultarPerfil(Long usuarioId);

    UsuarioDTO actualizarPerfil(Long usuarioId, DatosActualizacionPerfil datos);
}
