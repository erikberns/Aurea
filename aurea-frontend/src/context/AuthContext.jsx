import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { usuariosService } from "../services/usuariosService";
import { getToken, setToken } from "../api/httpClient";

const SESSION_KEY = "aurea_session_usuario";
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  // Al montar el provider, restauramos la sesión desde localStorage.
  // (Del lado del backend, ServicioDeUsuarios es stateless: esta
  // "restauración" es exclusivamente del cliente, cada request real
  // seguirá viajando con el JWT en el header Authorization.)
  useEffect(() => {
    const raw = localStorage.getItem(SESSION_KEY);
    if (raw && getToken()) {
      setUsuario(JSON.parse(raw));
    }
    setCargando(false);
  }, []);

  const iniciarSesion = useCallback(async (credenciales) => {
    const { token, usuario: u } = await usuariosService.autenticar(credenciales);
    setToken(token);
    localStorage.setItem(SESSION_KEY, JSON.stringify(u));
    setUsuario(u);
    return u;
  }, []);

  const registrarse = useCallback(async (datosRegistro) => {
    return usuariosService.registrarCliente(datosRegistro);
  }, []);

  const cerrarSesion = useCallback(() => {
    setToken(null);
    localStorage.removeItem(SESSION_KEY);
    setUsuario(null);
  }, []);

  const actualizarUsuarioLocal = useCallback((datos) => {
    setUsuario((prev) => {
      const actualizado = { ...prev, ...datos };
      localStorage.setItem(SESSION_KEY, JSON.stringify(actualizado));
      return actualizado;
    });
  }, []);

  const value = {
    usuario,
    estaAutenticado: !!usuario,
    cargando,
    iniciarSesion,
    registrarse,
    cerrarSesion,
    actualizarUsuarioLocal,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de <AuthProvider>");
  return ctx;
}
