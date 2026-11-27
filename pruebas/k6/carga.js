// Prueba de carga de Áurea Joyas con k6 (eje C de la entrega final).
// Objetivo: 50 usuarios virtuales durante 5 minutos, p95 < 500 ms y menos de 1 % de errores.
//
// Uso (con el sistema levantado con docker compose):
//   docker run --rm -i -v "${PWD}/pruebas:/pruebas" -e BASE_URL=http://host.docker.internal:5173 grafana/k6 run /pruebas/k6/carga.js
// Perfil corto para el CI (5 usuarios, 30 s): agregar -e PERFIL=humo
//
// Recorrido de cada usuario virtual: catálogo, categorías y detalle (público); en la primera iteración inicia
// sesión con una cuenta creada en setup() y desde ahí también consulta y modifica su bolsa de compras.
// No se compra: cada compra descuenta stock y, al agotarse, la tienda respondería 409 de forma esperable.
import http from "k6/http";
import { check, group, sleep } from "k6";
import exec from "k6/execution";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";

const BASE_URL = (__ENV.BASE_URL || "http://localhost:5173").replace(/\/$/, "");
const API = `${BASE_URL}/api`;
const HUMO = __ENV.PERFIL === "humo";
const CUENTAS = HUMO ? 5 : 50;

export const options = {
  scenarios: {
    compradores: HUMO
      ? { executor: "constant-vus", vus: 5, duration: "30s" }
      : {
          executor: "ramping-vus",
          startVUs: 0,
          stages: [
            { duration: "30s", target: 50 }, // rampa de subida
            { duration: "4m", target: 50 }, // carga sostenida
            { duration: "30s", target: 0 }, // rampa de bajada
          ],
          gracefulRampDown: "10s",
        },
  },
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    "http_req_duration{endpoint:catalogo}": ["p(95)<500"],
    "http_req_duration{endpoint:detalle}": ["p(95)<500"],
    "http_req_duration{endpoint:carrito}": ["p(95)<500"],
  },
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

function tokenCsrf() {
  const r = http.get(`${API}/csrf`, { tags: { endpoint: "csrf" } });
  return r.json("token");
}

// Crea las cuentas de prueba una sola vez (BCrypt es costoso a propósito; no se mide acá).
export function setup() {
  const productos = http.get(`${API}/productos`).json().filter((p) => p.active !== false && p.stock > 0);
  if (productos.length === 0) throw new Error("El catálogo está vacío: levantar el sistema con los datos de ejemplo.");
  const prefijo = `k6-${Date.now()}`;
  const cuentas = [];
  for (let i = 0; i < CUENTAS; i++) {
    const email = `${prefijo}-${i}@example.com`;
    const csrf = tokenCsrf();
    const r = http.post(
      `${API}/usuarios`,
      JSON.stringify({ nombre: "Carga", apellido: `Usuario ${i}`, email, contrasena: "ClaveDeCarga123" }),
      { headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf } }
    );
    if (r.status !== 201) throw new Error(`No se pudo registrar la cuenta ${i}: HTTP ${r.status}`);
    cuentas.push(email);
    http.cookieJar().clear(BASE_URL);
  }
  return { cuentas, ids: productos.map((p) => p.id) };
}

let bearer = null; // estado por usuario virtual

function iniciarSesion(email) {
  const csrf = tokenCsrf();
  const r = http.post(
    `${API}/usuarios/autenticar`,
    JSON.stringify({ email, contrasena: "ClaveDeCarga123" }),
    { headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf }, tags: { endpoint: "login" } }
  );
  check(r, { "login 200": (x) => x.status === 200 });
  return r.headers.Authorization || null;
}

export default function (datos) {
  const id = datos.ids[Math.floor(Math.random() * datos.ids.length)];

  group("catálogo público", () => {
    const lista = http.get(`${API}/productos`, { tags: { endpoint: "catalogo" } });
    check(lista, { "catálogo 200": (r) => r.status === 200, "catálogo con productos": (r) => r.json().length > 0 });
    const categorias = http.get(`${API}/productos/categorias`, { tags: { endpoint: "categorias" } });
    check(categorias, { "categorías 200": (r) => r.status === 200 });
    const detalle = http.get(`${API}/productos/${id}`, { tags: { endpoint: "detalle" } });
    check(detalle, { "detalle 200": (r) => r.status === 200, "detalle con ETag": (r) => !!r.headers.Etag });
  });
  sleep(1);

  if (bearer === null) {
    const email = datos.cuentas[(exec.vu.idInTest - 1) % datos.cuentas.length];
    bearer = iniciarSesion(email) || "";
  }
  if (!bearer) return;

  group("bolsa de compras", () => {
    const auth = { Authorization: bearer, "Content-Type": "application/json" };
    const agregar = http.post(`${API}/carrito/items`, JSON.stringify({ productoId: id, cantidad: 1 }), {
      headers: auth,
      tags: { endpoint: "carrito" },
    });
    check(agregar, { "agregar 200": (r) => r.status === 200 });
    const bolsa = http.get(`${API}/carrito`, { headers: auth, tags: { endpoint: "carrito" } });
    check(bolsa, { "bolsa 200": (r) => r.status === 200 });
    const vaciar = http.del(`${API}/carrito`, null, { headers: auth, tags: { endpoint: "carrito" } });
    check(vaciar, { "vaciar 200": (r) => r.status === 200 });
    const pedidos = http.get(`${API}/ordenes/mis-pedidos`, { headers: auth, tags: { endpoint: "pedidos" } });
    check(pedidos, { "mis pedidos 200": (r) => r.status === 200 });
  });
  sleep(1);
}

export function handleSummary(data) {
  const carpeta = __ENV.RESULTADOS || "/pruebas/resultados";
  return {
    stdout: textSummary(data, { indent: " ", enableColors: false }),
    [`${carpeta}/k6-${HUMO ? "humo" : "carga"}.json`]: JSON.stringify(data, null, 2),
  };
}
