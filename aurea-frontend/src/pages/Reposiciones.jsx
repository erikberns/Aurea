import { useCallback, useEffect, useRef, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { httpRequest } from "../api/httpClient";

const codigos = ["ANILLO-PLATA", "COLLAR-ORO", "PULSERA-PLATA"];
const fecha = valor => new Date(valor).toLocaleString("es-AR");
const campo = "w-full rounded border border-outline-variant bg-surface p-3";
const boton = "rounded bg-primary px-4 py-2 text-on-primary disabled:opacity-50";

export default function Reposiciones() {
  const { usuario, cargando } = useAuth();
  const admin = usuario?.rol === "ADMIN" || usuario?.rol === "ROLE_ADMIN";
  const [productos, setProductos] = useState([]);
  const [reposiciones, setReposiciones] = useState([]);
  const [avisos, setAvisos] = useState([]);
  const [productoId, setProductoId] = useState("");
  const [codigo, setCodigo] = useState(codigos[0]);
  const [cantidad, setCantidad] = useState(1);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [recepcionPendiente, setRecepcionPendiente] = useState(null);
  // Conserva el identificador si se pierde la respuesta HTTP: reintentar no crea otra solicitud.
  const solicitud = useRef(null);
  const recargar = useCallback(async () => {
    const [p, r, n] = await Promise.all([
      httpRequest("/productos"), httpRequest("/reposiciones"), httpRequest("/notificaciones"),
    ]);
    setProductos(p); setReposiciones(r); setAvisos(n);
  }, []);
  useEffect(() => {
    if (!admin) return;
    let activo = true;
    const cargar = () => recargar().catch(e => { if (activo) setError(e.message); });
    cargar();
    const timer = setInterval(cargar, 5000);
    return () => { activo = false; clearInterval(timer); };
  }, [admin, recargar]);
  async function ejecutar(accion) {
    setOcupado(true); setError(""); setInfo("");
    try { await accion(); await recargar(); }
    catch (e) { setError(e.message); }
    finally { setOcupado(false); }
  }
  async function solicitar(e) {
    e.preventDefault();
    await ejecutar(async () => {
      const datos = { productoId: Number(productoId), codigoProveedor: codigo, cantidad: Number(cantidad) };
      const firma = JSON.stringify(datos);
      if (!solicitud.current || solicitud.current.firma !== firma)
        solicitud.current = { firma, id: crypto.randomUUID() };
      const r = await httpRequest("/reposiciones", {
        method: "POST", body: { ...datos, idSolicitud: solicitud.current.id },
      });
      solicitud.current = null;
      setInfo(r.estado === "SOLICITADA"
        ? "Solicitud guardada. La respuesta del proveedor está pendiente; usá Reintentar consulta."
        : r.estado === "ACEPTADA" ? "El proveedor aceptó la reposición. El stock cambiará al registrar la recepción."
          : "El proveedor rechazó la solicitud por falta de disponibilidad.");
    });
  }
  if (cargando) return <p className="p-8">Cargando…</p>;
  if (!admin) return <Navigate to="/" replace />;
  return (
    <section className="mx-auto max-w-6xl px-4 py-10 text-on-surface">
      <h1 className="mb-2 text-3xl font-semibold">Reposiciones de proveedores</h1>
      <p className="mb-8 text-on-surface-variant">Solicitá mercadería y registrá su llegada para actualizar el inventario.</p>
      {error && <p role="alert" className="mb-4 rounded border border-error p-3 text-error">{error}</p>}
      {info && <p role="status" className="mb-4 rounded bg-surface-container p-3">{info}</p>}
      <form onSubmit={solicitar} className="mb-10 grid gap-4 rounded border border-outline-variant p-5 md:grid-cols-3">
        <label>Producto de Áurea
          <select required value={productoId} onChange={e => setProductoId(e.target.value)} className={campo} disabled={ocupado}>
            <option value="">Seleccionar producto</option>
            {productos.map(p => <option key={p.id} value={p.id}>{p.name} — stock: {p.stock}</option>)}
          </select>
        </label>
        <label>Código del proveedor
          <select value={codigo} onChange={e => setCodigo(e.target.value)} className={campo} disabled={ocupado}>
            {codigos.map(c => <option key={c}>{c}</option>)}
          </select>
        </label>
        <label>Cantidad
          <input type="number" min="1" max="10000" required value={cantidad}
            onChange={e => setCantidad(e.target.value)} className={campo} disabled={ocupado} />
        </label>
        <div className="flex flex-wrap gap-3 md:col-span-3">
          <button type="button" className={boton} disabled={ocupado} onClick={() => ejecutar(async () => {
            const r = await httpRequest("/reposiciones/disponibilidad?codigo=" + encodeURIComponent(codigo));
            setInfo("Disponibilidad del proveedor: " + r.disponibles + " unidades de " + codigo + ".");
          })}>Consultar disponibilidad</button>
          <button className={boton} disabled={ocupado || !productoId}>Solicitar reposición</button>
        </div>
      </form>
      <h2 className="mb-4 text-xl font-semibold">Seguimiento</h2>
      {reposiciones.length === 0 && <p className="mb-6">Todavía no hay reposiciones.</p>}
      <div className="space-y-4">
        {reposiciones.map(r => (
          <article key={r.id} className="rounded border border-outline-variant p-4">
            <div className="flex flex-wrap justify-between gap-3">
              <div>
                <h3 className="font-semibold">{productos.find(p => p.id === r.productoId)?.name || "Producto #" + r.productoId} · {r.cantidad} unidades</h3>
                <p className="text-sm text-on-surface-variant">{r.codigoProveedor} · {fecha(r.creada)}</p>
                <p className="mt-2">{r.estado === "RECIBIDA"
                  ? r.inventarioActualizado ? "Recibida · inventario actualizado" : "Recibida · actualización de inventario pendiente"
                  : r.estado === "SOLICITADA" ? "Respuesta del proveedor pendiente" : r.estado === "ACEPTADA" ? "Aceptada · esperando mercadería" : "Rechazada"}</p>
                {r.detalle && <p className="mt-2 text-sm">{r.detalle}</p>}
              </div>
              <div>
                {r.estado === "SOLICITADA" && <button className={boton} disabled={ocupado}
                  onClick={() => ejecutar(() => httpRequest("/reposiciones/" + r.id + "/sincronizar", { method: "POST" }))}>Reintentar consulta</button>}
                {r.estado === "ACEPTADA" && (recepcionPendiente === r.id ? <div className="space-y-3">
                  <p>¿Confirmás que recibiste físicamente esta mercadería?</p>
                  <div className="flex flex-wrap gap-3">
                    <button className={boton} disabled={ocupado} onClick={() => ejecutar(async () => {
                      await httpRequest("/reposiciones/" + r.id + "/recibir", { method: "POST" });
                      setRecepcionPendiente(null);
                    })}>Confirmar recepción</button>
                    <button className={boton} disabled={ocupado} onClick={() => setRecepcionPendiente(null)}>Volver</button>
                  </div>
                </div> : <button className={boton} disabled={ocupado}
                  onClick={() => setRecepcionPendiente(r.id)}>Registrar recepción</button>)}
              </div>
            </div>
            <p className="mt-2 break-all text-xs text-on-surface-variant">Solicitud: {r.id}</p>
          </article>
        ))}
      </div>
      <h2 className="mb-4 mt-10 text-xl font-semibold">Avisos de recepción</h2>
      {avisos.length === 0 ? <p>No hay avisos todavía.</p> : <ul className="space-y-3">
        {avisos.map(n => <li key={n.id} className="rounded bg-surface-container p-4">
          <p>{n.mensaje}</p><p className="mt-1 text-sm text-on-surface-variant">{fecha(n.creada)}</p>
        </li>)}
      </ul>}
    </section>
  );
}
