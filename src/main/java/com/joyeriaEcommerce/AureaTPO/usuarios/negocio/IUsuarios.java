package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

public interface IUsuarios {

    UsuarioDTO registrarCliente(DatosRegistro datos);

    UsuarioDTO autenticar(Credenciales credenciales);

    UsuarioDTO consultarPerfil(Long usuarioId);

    UsuarioDTO actualizarPerfil(Long usuarioId, DatosActualizacionPerfil datos);
}
