// Corre la colección de Postman con Newman contra el sistema levantado.
// Variables: BASE_URL (por defecto http://localhost:5173), ADMIN_EMAIL y ADMIN_PASSWORD (los del .env).
import newman from "newman";
import { mkdirSync } from "node:fs";

mkdirSync("../resultados", { recursive: true });
const envVar = [
  { key: "baseUrl", value: (process.env.BASE_URL || "http://localhost:5173").replace(/\/$/, "") },
  { key: "adminEmail", value: process.env.ADMIN_EMAIL || "admin@aurea.com" },
  { key: "adminPassword", value: process.env.ADMIN_PASSWORD || "" },
];
if (!process.env.ADMIN_PASSWORD) console.warn("Aviso: sin ADMIN_PASSWORD fallan los casos de la carpeta de administración.");

newman.run(
  {
    collection: "../newman/aurea.postman_collection.json",
    environment: "../newman/entorno-local.postman_environment.json",
    envVar,
    reporters: ["cli", "junit", "json"],
    reporter: {
      junit: { export: "../resultados/newman-junit.xml" },
      json: { export: "../resultados/newman.json" },
    },
  },
  (err, resumen) => {
    if (err) { console.error(err); process.exit(1); }
    const fallas = resumen.run.failures.length;
    const s = resumen.run.stats;
    console.log(`Peticiones: ${s.requests.total} · aserciones: ${s.assertions.total} · fallidas: ${s.assertions.failed}`);
    process.exit(fallas > 0 ? 1 : 0);
  }
);
