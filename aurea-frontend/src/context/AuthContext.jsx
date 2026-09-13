import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { usuariosService } from "../services/usuariosService";
import { httpRequest } from "../api/httpClient";
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);
  useEffect(() => {
    let activo = true;
    httpRequest("/usuarios/sesion").then(u => { if (activo) setUsuario(u); })
      .catch(() => { if (activo) setUsuario(null); })
      .finally(() => { if (activo) setCargando(false); });
    const vencida = () => setUsuario(null);
    window.addEventListener("aurea:sesion-vencida", vencida);
    return () => { activo = false; window.removeEventListener("aurea:sesion-vencida", vencida); };
  }, []);
  const iniciarSesion = useCallback(async datos => {
    const u = await usuariosService.autenticar(datos); setUsuario(u); return u;
  }, []);
  const registrarse = useCallback(async datos => {
    const u = await usuariosService.registrarCliente(datos); setUsuario(u); return u;
  }, []);
  const cerrarSesion = useCallback(async () => {
    try {
      await httpRequest("/usuarios/salir", { method: "POST" });
      setUsuario(null);
      window.dispatchEvent(new Event("aurea:carrito-cambio"));
      return true;
    } catch (error) { alert(error.message); return false; }
  }, []);
  const actualizarUsuarioLocal = useCallback(datos => setUsuario(prev => ({ ...prev, ...datos })), []);
  return <AuthContext.Provider value={{ usuario, estaAutenticado: !!usuario, cargando,
    iniciarSesion, registrarse, cerrarSesion, actualizarUsuarioLocal }}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de AuthProvider");
  return ctx;
}
