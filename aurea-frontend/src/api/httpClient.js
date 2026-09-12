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

function getCsrfToken() {
  const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
  if (match) {
    return decodeURIComponent(match[2]);
  }
  return null;
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
 *  - credentials: "include" para enviar cookies (JSESSIONID)
 *  - X-XSRF-TOKEN para métodos mutables
 *  - Parseo de errores del backend en un formato consistente
 */
export async function httpRequest(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      credentials: "include",
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