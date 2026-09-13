import { httpRequest } from "../api/httpClient";

export const usuariosService = {
  registrarCliente: (datosRegistro) =>
    httpRequest("/usuarios", { method: "POST", body: datosRegistro, auth: false }),

  autenticar: (credenciales) =>
    httpRequest("/usuarios/autenticar", { method: "POST", body: credenciales, auth: false }),

  consultarPerfil: (usuarioId) => httpRequest(`/usuarios/${usuarioId}`),

  actualizarPerfil: (usuarioId, datosPerfil) =>
    httpRequest(`/usuarios/${usuarioId}`, { method: "PATCH", body: datosPerfil }),

  cambiarContrasena: (usuarioId, contrasenaActual, contrasenaNueva) =>
    httpRequest(`/usuarios/${usuarioId}/contrasena`, {
      method: "PATCH",
      body: { contrasenaActual, contrasenaNueva },
    }),

  asignarRol: (usuarioId, rol) =>
    httpRequest(`/usuarios/${usuarioId}/rol`, { method: "PATCH", body: { rol } }),
};
