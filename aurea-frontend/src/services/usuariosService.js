// ---------------------------------------------------------------------------
// usuariosService.js
// Implementa, del lado del frontend, cada operación de la interfaz iUsuarios
// definida en la documentación del TP (ServicioDeUsuarios):
//   registrarCliente, autenticar, consultarPerfil, actualizarPerfil,
//   cambiarContrasena, asignarRol
//
// El servicio expone siempre la misma firma sin importar el origen real de
// los datos. Dos "adaptadores" implementan ese contrato:
//   - realAdapter -> llama a la API REST de Spring Boot (uso normal)
//   - mockAdapter -> simula el backend en localStorage (demo / desarrollo
//     sin backend levantado)
// Esto es una aplicación del patrón Adapter en el frontend: las páginas de
// React (capa de presentación) llaman siempre a `usuariosService`, sin saber
// si por detrás hay un servidor real o el mock.
// ---------------------------------------------------------------------------
import { httpRequest, setToken } from "../api/httpClient";

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? "true") === "true";
const MOCK_DB_KEY = "aurea_mock_usuarios_db";

// ---- Adaptador real: consume la API REST de ServicioDeUsuarios -----------
const realAdapter = {
  registrarCliente: (datosRegistro) =>
    httpRequest("/usuarios", { method: "POST", body: datosRegistro, auth: false }),

  autenticar: (credenciales) =>
    httpRequest("/usuarios/autenticar", { method: "POST", body: credenciales, auth: false }),

  consultarPerfil: (usuarioId) => httpRequest(`/usuarios/${usuarioId}`),

  actualizarPerfil: (usuarioId, datosPerfil) =>
    httpRequest(`/usuarios/${usuarioId}`, { method: "PUT", body: datosPerfil }),

  cambiarContrasena: (usuarioId, contrasenaActual, contrasenaNueva) =>
    httpRequest(`/usuarios/${usuarioId}/contrasena`, {
      method: "PATCH",
      body: { contrasenaActual, contrasenaNueva },
    }),

  asignarRol: (usuarioId, rol) =>
    httpRequest(`/usuarios/${usuarioId}/rol`, { method: "PATCH", body: { rol } }),
};

// ---- Adaptador mock: simula ServicioDeUsuarios en localStorage -----------
function readDb() {
  return JSON.parse(localStorage.getItem(MOCK_DB_KEY) || "[]");
}
function writeDb(db) {
  localStorage.setItem(MOCK_DB_KEY, JSON.stringify(db));
}
function delay(ms = 350) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
function fakeToken(usuarioId) {
  return btoa(`${usuarioId}.${Date.now()}`);
}

const mockAdapter = {
  async registrarCliente(datosRegistro) {
    await delay();
    const db = readDb();
    if (db.some((u) => u.email === datosRegistro.email)) {
      const err = new Error("Ya existe una cuenta registrada con ese email.");
      err.status = 409;
      throw err;
    }
    const usuario = {
      id: crypto.randomUUID(),
      nombre: datosRegistro.nombre,
      apellido: datosRegistro.apellido,
      email: datosRegistro.email,
      contrasena: datosRegistro.contrasena, // demo únicamente: en el backend real se hashea con BCrypt
      rol: "CLIENTE",
      fechaAlta: new Date().toISOString(),
    };
    db.push(usuario);
    writeDb(db);
    return { id: usuario.id, nombre: usuario.nombre, email: usuario.email, rol: usuario.rol };
  },

  async autenticar(credenciales) {
    await delay();
    const db = readDb();
    const usuario = db.find(
      (u) => u.email === credenciales.email && u.contrasena === credenciales.contrasena
    );
    if (!usuario) {
      const err = new Error("Email o contraseña incorrectos.");
      err.status = 401;
      throw err;
    }
    const token = fakeToken(usuario.id);
    setToken(token);
    return {
      token,
      usuario: { id: usuario.id, nombre: usuario.nombre, email: usuario.email, rol: usuario.rol },
    };
  },

  async consultarPerfil(usuarioId) {
    await delay();
    const db = readDb();
    const usuario = db.find((u) => u.id === usuarioId);
    if (!usuario) throw new Error("Usuario no encontrado.");
    const { contrasena, ...perfil } = usuario;
    return perfil;
  },

  async actualizarPerfil(usuarioId, datosPerfil) {
    await delay();
    const db = readDb();
    const idx = db.findIndex((u) => u.id === usuarioId);
    if (idx === -1) throw new Error("Usuario no encontrado.");
    db[idx] = { ...db[idx], ...datosPerfil };
    writeDb(db);
    const { contrasena, ...perfil } = db[idx];
    return perfil;
  },

  async cambiarContrasena(usuarioId, contrasenaActual, contrasenaNueva) {
    await delay();
    const db = readDb();
    const idx = db.findIndex((u) => u.id === usuarioId);
    if (idx === -1) throw new Error("Usuario no encontrado.");
    if (db[idx].contrasena !== contrasenaActual) {
      const err = new Error("La contraseña actual no es correcta.");
      err.status = 400;
      throw err;
    }
    db[idx].contrasena = contrasenaNueva;
    writeDb(db);
    return { ok: true };
  },

  async asignarRol(usuarioId, rol) {
    await delay();
    const db = readDb();
    const idx = db.findIndex((u) => u.id === usuarioId);
    if (idx === -1) throw new Error("Usuario no encontrado.");
    db[idx].rol = rol;
    writeDb(db);
    return { ok: true };
  },
};

const adapter = USE_MOCK ? mockAdapter : realAdapter;

export const usuariosService = {
  registrarCliente: (datosRegistro) => adapter.registrarCliente(datosRegistro),
  autenticar: (credenciales) => adapter.autenticar(credenciales),
  consultarPerfil: (usuarioId) => adapter.consultarPerfil(usuarioId),
  actualizarPerfil: (usuarioId, datosPerfil) => adapter.actualizarPerfil(usuarioId, datosPerfil),
  cambiarContrasena: (usuarioId, actual, nueva) =>
    adapter.cambiarContrasena(usuarioId, actual, nueva),
  asignarRol: (usuarioId, rol) => adapter.asignarRol(usuarioId, rol),
  usingMock: USE_MOCK,
};
