// Utilidades compartidas por el panel de administración.

/** El backend devuelve el rol como "ADMIN"; se acepta también el prefijo de Spring Security. */
export const esAdmin = (usuario) => usuario?.rol === "ADMIN" || usuario?.rol === "ROLE_ADMIN";

/** Por debajo de este stock una pieza se considera crítica. */
export const UMBRAL_STOCK_CRITICO = 5;

const numero = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 });
export const moneda = (valor) => `$ ${numero.format(Number(valor) || 0)}`;

export const fechaHora = (valor) =>
  valor ? new Date(valor).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" }) : "—";

/** Fechas LocalDate ("2026-11-15") sin corrimiento por zona horaria. */
export const fecha = (valor) => {
  if (!valor) return "—";
  const texto = String(valor);
  return new Date(texto.length === 10 ? `${texto}T00:00:00` : texto).toLocaleDateString("es-AR");
};

export function estadoStock(producto, umbral = UMBRAL_STOCK_CRITICO) {
  if (producto.active === false) return { clave: "archivado", texto: "Archivada", tono: "neutro" };
  const stock = Number(producto.stock) || 0;
  if (stock === 0) return { clave: "agotado", texto: "Agotado", tono: "neutro" };
  if (stock <= umbral) return { clave: "critico", texto: "Stock crítico", tono: "alerta" };
  return { clave: "disponible", texto: "Disponible", tono: "exito" };
}

export const ESTADOS_PEDIDO = {
  PENDING: { texto: "Pendiente", tono: "pendiente" },
  CONFIRMED: { texto: "Confirmado", tono: "info" },
  COMPLETED: { texto: "Completado", tono: "exito" },
  CANCELLED: { texto: "Cancelado", tono: "neutro" },
};

export const ESTADOS_PAGO = {
  AUTORIZADO: { texto: "Autorizado", tono: "pendiente" },
  CAPTURADO: { texto: "Cobrado", tono: "exito" },
  RECHAZADO: { texto: "Rechazado", tono: "alerta" },
  ANULADO: { texto: "Anulado", tono: "neutro" },
  CAPTURA_PENDIENTE: { texto: "Cobro pendiente", tono: "alerta" },
  ANULACION_PENDIENTE: { texto: "Anulación pendiente", tono: "alerta" },
};

export const ESTADOS_REPOSICION = {
  SOLICITADA: { texto: "Solicitada · respuesta pendiente", tono: "pendiente" },
  ACEPTADA: { texto: "Aceptada · esperando mercadería", tono: "info" },
  RECHAZADA: { texto: "Rechazada por el proveedor", tono: "alerta" },
  RECIBIDA: { texto: "Recibida", tono: "exito" },
};

/** Identificador corto y legible para UUIDs de solicitudes. */
export const codigoCorto = (id) => `#REP-${String(id).slice(0, 8).toUpperCase()}`;
