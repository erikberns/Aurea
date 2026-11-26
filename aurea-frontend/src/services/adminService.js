import { httpRequest } from "../api/httpClient";

const q = (valor) => encodeURIComponent(valor);

/**
 * Clientes HTTP del panel de administración. La autorización real la aplica el backend
 * (@PreAuthorize("hasRole('ADMIN')")): ocultar pantallas en el cliente es solo experiencia de uso.
 */
export const adminService = {
  productos: () => httpRequest("/productos"),
  producto: (id) => httpRequest(`/productos/${id}`),
  categorias: () => httpRequest("/productos/categorias"),
  crearProducto: (datos) => httpRequest("/productos", { method: "POST", body: datos }),
  actualizarProducto: (id, datos) => httpRequest(`/productos/${id}`, { method: "PUT", body: datos }),
  actualizarPrecio: (id, precio) => httpRequest(`/productos/${id}/precio?newPrice=${q(precio)}`, { method: "PATCH" }),
  actualizarStock: (id, stock) => httpRequest(`/productos/${id}/inventario?newStock=${q(stock)}`, { method: "PATCH" }),
  cambiarEstado: (id, activo) => httpRequest(`/productos/${id}/estado?activo=${activo}`, { method: "PATCH" }),
  sugerirDescripcion: (datos) => httpRequest("/productos/sugerencia-ia", { method: "POST", body: datos }),
  estadoAsistenteIA: () => httpRequest("/productos/sugerencia-ia/estado"),
  trazasIA: () => httpRequest("/ia/trazas"),

  pedidos: () => httpRequest("/ordenes"),
  pagos: () => httpRequest("/pagos"),
  aprobarDevolucion: (id) => httpRequest(`/ordenes/${q(id)}/devolucion/aprobar`, { method: "POST" }),

  usuarios: () => httpRequest("/usuarios"),
  cambiarRol: (id, rol) => httpRequest(`/usuarios/${id}/rol`, { method: "PATCH", body: { rol } }),

  avisos: () => httpRequest("/notificaciones"),

  reposiciones: () => httpRequest("/reposiciones"),
  disponibilidad: (codigo) => httpRequest(`/reposiciones/disponibilidad?codigo=${q(codigo)}`),
  solicitarReposicion: (datos) => httpRequest("/reposiciones", { method: "POST", body: datos }),
  sincronizarReposicion: (id) => httpRequest(`/reposiciones/${q(id)}/sincronizar`, { method: "POST" }),
  recibirReposicion: (id) => httpRequest(`/reposiciones/${q(id)}/recibir`, { method: "POST" }),
};
