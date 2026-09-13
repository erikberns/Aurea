export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
export class ApiError extends Error {
  constructor(message, status, data) { super(message); this.status = status; this.data = data; }
}
// Cada mutacion obtiene un token actual, incluso despues de logout o una sesion nueva.
export async function httpRequest(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  let response;
  try {
    if (!["GET", "HEAD"].includes(method)) {
      const csrfResponse = await fetch(`${API_BASE_URL}/csrf`, { credentials: "include", cache: "no-store" });
      if (!csrfResponse.ok) throw new ApiError("No se pudo iniciar la solicitud segura", csrfResponse.status);
      const csrf = await csrfResponse.json();
      headers[csrf.headerName] = csrf.token;
    }
    response = await fetch(`${API_BASE_URL}${path}`, {
      method, headers, credentials: "include", cache: "no-store",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError("No se pudo contactar al servidor de Áurea.", 0, null);
  }
  const data = response.headers.get("content-type")?.includes("application/json")
    ? await response.json().catch(() => null) : null;
  if (!response.ok) {
    if (response.status === 401) window.dispatchEvent(new Event("aurea:sesion-vencida"));
    throw new ApiError(data?.mensaje || data?.message || `Error ${response.status}`, response.status, data);
  }
  return data;
}
