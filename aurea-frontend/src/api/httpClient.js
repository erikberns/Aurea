// ---------------------------------------------------------------------------
// httpClient.js
// Capa de acceso a datos del frontend: encapsula la comunicación HTTP con la
// API REST expuesta por ServicioDeUsuarios (Spring Boot).
// Al centralizar fetch() acá, el resto de la app (páginas y componentes) no
// conoce detalles de transporte (headers, JWT, manejo de errores HTTP).
// ---------------------------------------------------------------------------

// Base URL del backend. Se puede sobreescribir con una variable de entorno
// de Vite (.env -> VITE_API_BASE_URL) sin tocar código.
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const TOKEN_KEY = "aurea_access_token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

/**
 * Wrapper único sobre fetch(). Agrega automáticamente:
 *  - Content-Type JSON
 *  - Authorization: Bearer <token> (cuando ServicioDeUsuarios ya emitió un JWT)
 *  - Parseo de errores del backend en un formato consistente
 */
export async function httpRequest(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  const token = getToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw new ApiError(
      "No se pudo contactar al ServicioDeUsuarios. Verificá que el backend esté levantado en " +
        API_BASE_URL,
      0,
      null
    );
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await response.json().catch(() => null) : null;

  if (!response.ok) {
    throw new ApiError(
      data?.message || data?.mensaje || `Error ${response.status} al comunicarse con el servidor`,
      response.status,
      data
    );
  }
  return data;
}

export { ApiError };