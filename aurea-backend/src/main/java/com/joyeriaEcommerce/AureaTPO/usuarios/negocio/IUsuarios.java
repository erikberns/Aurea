package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

public interface IUsuarios {

    UsuarioDTO registrarCliente(DatosRegistro datos);

    UsuarioDTO autenticar(Credenciales credenciales);

    UsuarioDTO consultarPerfil(Long usuarioId);

    UsuarioDTO consultarPerfilPorEmail(String email);

    UsuarioDTO actualizarPerfil(Long usuarioId, DatosActualizacionPerfil datos);

    void cambiarContrasena(Long usuarioId, DatosCambioContrasena datos);

    java.util.List<UsuarioDTO> obtenerTodos();

    UsuarioDTO asignarRol(Long usuarioId, String rol);
}
