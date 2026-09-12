import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { usuariosService } from "../services/usuariosService";

const SESSION_KEY = "aurea_session_usuario";
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  // Al montar el provider, restauramos la sesión desde localStorage.
  // Ahora la seguridad se maneja con la cookie JSESSIONID,
  // pero guardamos el perfil localmente para tener nombre y rol.
  useEffect(() => {
    const raw = localStorage.getItem(SESSION_KEY);
    if (raw) {
      setUsuario(JSON.parse(raw));
    }
    setCargando(false);
  }, []);

  const iniciarSesion = useCallback(async (credenciales) => {
    const u = await usuariosService.autenticar(credenciales);
    localStorage.setItem(SESSION_KEY, JSON.stringify(u));
    setUsuario(u);
    return u;
  }, []);

  const registrarse = useCallback(async (datosRegistro) => {
    const u = await usuariosService.registrarCliente(datosRegistro);
    localStorage.setItem(SESSION_KEY, JSON.stringify(u));
    setUsuario(u);
    return u;
  }, []);

  const cerrarSesion = useCallback(async () => {
    try {
      await fetch("http://localhost:8080/api/usuarios/salir", { method: "POST" });
    } catch (e) {
      console.warn("No se pudo contactar al backend para logout.");
    }
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
