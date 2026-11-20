import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { esAdmin } from "../utils/admin";
import AureaLoader from "./AureaLoader";

/**
 * Réplica, del lado del cliente, de la seguridad declarativa que
 * ServicioDeUsuarios exigirá en el backend (autenticación + autorización
 * por rol) para operaciones sensibles. Esto NO reemplaza la verificación
 * real del backend (que siempre debe volver a validar la sesión y el rol),
 * es una mejora de experiencia: evita mostrarle una pantalla protegida a
 * quien no inició sesión.
 */
export default function ProtectedRoute({ children, rolRequerido }) {
  const { estaAutenticado, usuario, cargando } = useAuth();
  const location = useLocation();

  if (cargando) return <AureaLoader text="Verificando tu sesión..." fullScreen />;

  if (!estaAutenticado) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const autorizado = rolRequerido === "ADMIN" ? esAdmin(usuario) : usuario?.rol === rolRequerido;
  if (rolRequerido && !autorizado) {
    return <Navigate to="/cuenta" replace />;
  }

  return children;
}
