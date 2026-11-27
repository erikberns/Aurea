// Lighthouse sobre las páginas principales usando el Chromium que instala Playwright.
// Falla (código 1) si la accesibilidad de alguna página queda por debajo de 90.
// Uso: BASE_URL=http://localhost:5173 node lighthouse.mjs
import { chromium } from "@playwright/test";
import { spawnSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const BASE_URL = (process.env.BASE_URL || "http://localhost:5173").replace(/\/$/, "");
const SALIDA = "../resultados/lighthouse";
const PAGINAS = { inicio: "/", catalogo: "/catalogo", detalle: "/productos/1", login: "/login" };
const MINIMO_ACCESIBILIDAD = 90;

mkdirSync(SALIDA, { recursive: true });
const cli = require.resolve("lighthouse/cli/index.js");
const chrome = chromium.executablePath();
const filas = [];
let falla = false;

for (const [nombre, ruta] of Object.entries(PAGINAS)) {
  const base = `${SALIDA}/${nombre}`;
  const r = spawnSync(process.execPath, [
    cli, `${BASE_URL}${ruta}`,
    "--quiet", "--output=json", "--output=html", `--output-path=${base}`,
    "--only-categories=performance,accessibility,best-practices,seo",
    "--chrome-flags=--headless=new --no-sandbox",
  ], { env: { ...process.env, CHROME_PATH: chrome }, stdio: "inherit" });
  if (r.status !== 0) { console.error(`Lighthouse falló en ${nombre}`); falla = true; continue; }
  const rep = JSON.parse(readFileSync(`${base}.report.json`, "utf8"));
  const p = (c) => Math.round((rep.categories[c]?.score ?? 0) * 100);
  const fila = { pagina: nombre, rendimiento: p("performance"), accesibilidad: p("accessibility"), buenasPracticas: p("best-practices"), seo: p("seo") };
  if (fila.accesibilidad < MINIMO_ACCESIBILIDAD) {
    falla = true;
    const fallas = Object.values(rep.audits)
      .filter((a) => a.score === 0 && rep.categories.accessibility.auditRefs.some((ref) => ref.id === a.id))
      .map((a) => a.title);
    fila.problemasAccesibilidad = fallas;
  }
  filas.push(fila);
}

console.table(filas.map(({ problemasAccesibilidad, ...f }) => f));
for (const f of filas) if (f.problemasAccesibilidad) console.log(`Accesibilidad en ${f.pagina}:`, f.problemasAccesibilidad);
writeFileSync(`${SALIDA}/resumen.json`, JSON.stringify(filas, null, 2));
if (falla) { console.error(`Alguna página quedó por debajo de ${MINIMO_ACCESIBILIDAD} en accesibilidad.`); process.exit(1); }
console.log(`Accesibilidad de ${MINIMO_ACCESIBILIDAD} o más en todas las páginas.`);
