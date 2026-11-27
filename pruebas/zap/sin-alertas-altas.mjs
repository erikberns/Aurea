// Lee el reporte JSON de ZAP, imprime las alertas por nivel de riesgo y falla si hay alguna High.
import { readFileSync } from "node:fs";

const archivo = process.argv[2];
const reporte = JSON.parse(readFileSync(archivo, "utf8"));
const NIVELES = { 3: "High", 2: "Medium", 1: "Low", 0: "Informational" };
const alertas = (reporte.site || []).flatMap((s) => s.alerts || []);
const porNivel = { High: [], Medium: [], Low: [], Informational: [] };
for (const a of alertas) porNivel[NIVELES[a.riskcode] ?? "Informational"].push(`${a.name} (${a.count ?? a.instances?.length ?? "?"} instancias)`);

for (const [nivel, lista] of Object.entries(porNivel)) {
  console.log(`${nivel}: ${lista.length}`);
  for (const x of lista) console.log(`  - ${x}`);
}
if (porNivel.High.length > 0) {
  console.error(`ZAP encontró ${porNivel.High.length} alerta(s) High: no se cumple la condición de la entrega.`);
  process.exit(1);
}
console.log("Sin alertas High.");
