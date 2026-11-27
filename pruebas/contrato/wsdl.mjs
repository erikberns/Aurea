// Prueba de contrato SOAP: el WSDL publicado por el proveedor debe ofrecer exactamente lo que
// ProveedorSoapAdapter consume (namespace, operaciones, parámetros y campos de la respuesta).
// Uso: node pruebas/contrato/wsdl.mjs [URL del WSDL]   (por defecto http://localhost:8091/soap/proveedor?wsdl)
const URL_WSDL = process.argv[2] || process.env.WSDL_URL || "http://localhost:8091/soap/proveedor?wsdl";

// Lo que espera el adaptador del backend (ver ProveedorSoapAdapter.java).
const ESPERADO = {
  namespace: "urn:aurea:proveedor:v1",
  operaciones: {
    consultarDisponibilidad: ["codigoProducto"],
    solicitarReposicion: ["idSolicitud", "codigoProducto", "cantidad"],
    consultarEstado: ["idSolicitud"],
  },
  camposResultado: ["idSolicitud", "codigoProducto", "cantidad", "estado", "disponibles"],
};

async function texto(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`${url} respondió ${r.status}`);
  return r.text();
}

const errores = [];
const wsdl = await texto(URL_WSDL);
if (!wsdl.includes(`targetNamespace="${ESPERADO.namespace}"`)) errores.push(`El WSDL no declara el namespace ${ESPERADO.namespace}`);
for (const op of Object.keys(ESPERADO.operaciones)) {
  if (!new RegExp(`<(\\w+:)?operation[^>]+name="${op}"`).test(wsdl)) errores.push(`Falta la operación ${op}`);
}

// Los tipos suelen estar en un XSD aparte (JAX-WS: ?xsd=1); se juntan todos para buscar elementos.
const xsds = [...wsdl.matchAll(/schemaLocation="([^"]+)"/g)].map((m) => new URL(m[1].replaceAll("&amp;", "&"), URL_WSDL).href);
const esquemas = [wsdl, ...(await Promise.all(xsds.map(texto)))].join("\n");
const tieneElemento = (nombre) => new RegExp(`name="${nombre}"`).test(esquemas);
for (const [op, params] of Object.entries(ESPERADO.operaciones)) {
  if (!tieneElemento(op)) errores.push(`No hay elemento de entrada para ${op}`);
  if (!tieneElemento(`${op}Response`)) errores.push(`No hay elemento de respuesta ${op}Response`);
  for (const p of params) if (!tieneElemento(p)) errores.push(`${op}: falta el parámetro ${p}`);
}
for (const campo of ESPERADO.camposResultado) if (!tieneElemento(campo)) errores.push(`La respuesta no define el campo ${campo}`);

if (errores.length) {
  console.error("Contrato SOAP roto:\n - " + errores.join("\n - "));
  process.exit(1);
}
console.log(`Contrato SOAP correcto: ${Object.keys(ESPERADO.operaciones).length} operaciones, namespace ${ESPERADO.namespace}, ${xsds.length} XSD revisados.`);
