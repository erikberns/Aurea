import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService";
import { Boton, campo } from "./AdminUI";

/**
 * Asistente de descripciones con IA. Solo sugiere: el texto se muestra para revisar y recién
 * se copia al formulario cuando el administrador elige "Usar sugerencia" (humano en el circuito).
 * La respuesta se muestra como texto plano; nunca se interpreta como HTML.
 */
export default function AsistenteDescripcionIA({ nombre, onUsar }) {
  const [estado, setEstado] = useState(null);
  const [material, setMaterial] = useState("");
  const [detalles, setDetalles] = useState("");
  const [pidiendo, setPidiendo] = useState(false);
  const [sugerencia, setSugerencia] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let activo = true;
    adminService
      .estadoAsistenteIA()
      .then((e) => activo && setEstado(e))
      .catch(() => activo && setEstado({ habilitada: false }));
    return () => {
      activo = false;
    };
  }, []);

  if (estado && !estado.habilitada) {
    return (
      <p className="rounded-lg bg-surface-container p-3 font-body-sm text-body-sm text-on-surface-variant">
        El asistente de descripciones con IA está apagado. Podés completar la descripción a mano.
      </p>
    );
  }

  const pedir = async () => {
    setPidiendo(true);
    setError("");
    setSugerencia(null);
    try {
      setSugerencia(await adminService.sugerirDescripcion({ nombre: nombre.trim(), material: material.trim(), detalles: detalles.trim() }));
    } catch (e) {
      setError(e.message);
    } finally {
      setPidiendo(false);
    }
  };

  const deIA = sugerencia?.fuente === "ia";

  return (
    <div className="space-y-3 rounded-lg border border-outline-variant/40 bg-surface-container-low p-4">
      <p className="flex items-center gap-2 font-label-md text-label-md text-on-surface">
        <span className="material-symbols-outlined text-[18px] text-primary">auto_awesome</span>
        Asistente de descripción con IA
        {estado?.proveedor && (
          <span className="font-label-sm text-label-sm text-on-surface-variant">· {estado.proveedor}</span>
        )}
      </p>
      <p className="font-body-sm text-body-sm text-on-surface-variant">
        Cargá el material y los detalles de la pieza. Solo se envían estos datos del producto, nunca datos de clientes.
      </p>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1 block font-label-sm text-label-sm text-on-surface">Material</span>
          <input value={material} onChange={(e) => setMaterial(e.target.value)} maxLength={80} className={campo}
            placeholder="Plata 925" />
        </label>
        <label className="block">
          <span className="mb-1 block font-label-sm text-label-sm text-on-surface">Detalles (piedras, terminación, estilo)</span>
          <input value={detalles} onChange={(e) => setDetalles(e.target.value)} maxLength={400} className={campo}
            placeholder="Con un circón central, terminación pulida" />
        </label>
      </div>
      <Boton variante="contorno" icono="auto_awesome" onClick={pedir} disabled={pidiendo || !nombre.trim()}>
        {pidiendo ? "Pensando…" : "Sugerir con IA"}
      </Boton>
      {!nombre.trim() && (
        <p className="font-body-sm text-body-sm text-on-surface-variant">Primero escribí el nombre de la pieza.</p>
      )}
      {error && <p role="alert" className="font-body-sm text-body-sm text-error">{error}</p>}

      {sugerencia && (
        <div role="status" className="space-y-3 rounded-lg bg-surface-container-lowest p-4 shadow-sm">
          <p className={`font-label-sm text-label-sm uppercase ${deIA ? "text-primary" : "text-on-surface-variant"}`}>
            {deIA ? "Sugerencia generada con IA · revisala antes de guardar" : "Plantilla para completar a mano"}
          </p>
          <p className="whitespace-pre-line font-body-md text-body-md text-on-surface">{sugerencia.descripcion}</p>
          {sugerencia.categoria && (
            <p className="font-body-sm text-body-sm text-on-surface-variant">Categoría sugerida: {sugerencia.categoria}</p>
          )}
          {sugerencia.aviso && <p className="font-body-sm text-body-sm text-on-surface-variant">{sugerencia.aviso}</p>}
          <div className="flex flex-wrap gap-3">
            <Boton icono="check" onClick={() => { onUsar(sugerencia); setSugerencia(null); }}>
              Usar sugerencia
            </Boton>
            <Boton variante="secundario" icono="close" onClick={() => setSugerencia(null)}>
              Descartar
            </Boton>
          </div>
        </div>
      )}
    </div>
  );
}
