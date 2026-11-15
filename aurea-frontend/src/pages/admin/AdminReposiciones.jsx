import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { adminService } from "../../services/adminService";
import { imagenDeProducto } from "../../utils/imagenes";
import { ESTADOS_REPOSICION, codigoCorto, estadoStock, fechaHora } from "../../utils/admin";
import {
  Boton, Busqueda, Cargando, Chips, Criterio, Encabezado, Estado, Indicador, Mensaje, Tarjeta, Vacio, campo,
} from "../../components/admin/AdminUI";

/** Códigos del catálogo del proveedor mayorista (servicio SOAP externo, ver ProveedorSOAP). */
const CODIGOS_PROVEEDOR = ["ANILLO-PLATA", "COLLAR-ORO", "PULSERA-PLATA"];
const PASOS = [
  { clave: "solicitar", numero: 1, titulo: "Solicitar reposición", detalle: "Consulta y pedido al proveedor (SOAP)" },
  { clave: "seguimiento", numero: 2, titulo: "Seguimiento de órdenes", detalle: "Respuesta del proveedor y recepción" },
  { clave: "ingresos", numero: 3, titulo: "Registro de ingresos", detalle: "Mercadería recibida y stock acreditado" },
];

function descripcionReposicion(r) {
  if (r.estado === "RECIBIDA") {
    return r.inventarioActualizado
      ? `Ingreso verificado el ${fechaHora(r.recibida)}. Stock acreditado en la tienda (+${r.cantidad} un.).`
      : "Recepción registrada. El evento se publicó en el tópico y el inventario se está acreditando.";
  }
  if (r.estado === "ACEPTADA") {
    return "El proveedor aceptó la solicitud. El stock de la tienda cambia recién al registrar la recepción física.";
  }
  if (r.estado === "SOLICITADA") {
    return r.detalle || "La solicitud se guardó y la respuesta del proveedor está pendiente. Podés reintentar sin duplicarla.";
  }
  return r.detalle || "El proveedor no tenía disponibilidad suficiente.";
}

/**
 * Reposición de mercadería: integración síncrona SOAP con el proveedor mayorista (legado) y
 * recepción asíncrona: la outbox publica ReposicionRecibida en RabbitMQ (tópico) y los
 * suscriptores de Inventario y Avisos la consumen por separado.
 */
export default function AdminReposiciones() {
  const [params, setParams] = useSearchParams();
  const paso = PASOS.some((p) => p.clave === params.get("paso")) ? params.get("paso") : "solicitar";
  const [productos, setProductos] = useState(null);
  const [reposiciones, setReposiciones] = useState([]);
  const [productoId, setProductoId] = useState(params.get("producto") ?? "");
  const [codigo, setCodigo] = useState(CODIGOS_PROVEEDOR[0]);
  const [cantidad, setCantidad] = useState(12);
  const [disponibilidad, setDisponibilidad] = useState(null);
  const [filtro, setFiltro] = useState("TODAS");
  const [busqueda, setBusqueda] = useState("");
  const [confirmando, setConfirmando] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  // Conserva el identificador si se pierde la respuesta HTTP: reintentar no crea otra solicitud.
  const solicitud = useRef(null);

  const recargar = useCallback(async () => {
    const [p, r] = await Promise.all([adminService.productos(), adminService.reposiciones()]);
    setProductos(p);
    setReposiciones(r);
  }, []);

  useEffect(() => {
    let activo = true;
    const cargar = () => recargar().catch((e) => activo && setError(e.message));
    cargar();
    const timer = setInterval(cargar, 5000);
    return () => {
      activo = false;
      clearInterval(timer);
    };
  }, [recargar]);

  const irA = (clave) => {
    const nuevos = new URLSearchParams(params);
    nuevos.set("paso", clave);
    nuevos.delete("producto");
    setParams(nuevos, { replace: true });
  };

  async function ejecutar(accion) {
    setOcupado(true);
    setError("");
    setInfo("");
    try {
      await accion();
      await recargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setOcupado(false);
    }
  }

  const codigoValido = /^[A-Z0-9-]{1,64}$/.test(codigo);
  const cantidadValida = Number.isInteger(Number(cantidad)) && Number(cantidad) >= 1 && Number(cantidad) <= 10000;

  const consultar = () =>
    ejecutar(async () => {
      const r = await adminService.disponibilidad(codigo);
      setDisponibilidad({ codigo, unidades: r.disponibles });
    });

  const solicitar = (e) => {
    e.preventDefault();
    if (!productoId || !codigoValido || !cantidadValida) return;
    ejecutar(async () => {
      const datos = { productoId: Number(productoId), codigoProveedor: codigo, cantidad: Number(cantidad) };
      const firma = JSON.stringify(datos);
      if (!solicitud.current || solicitud.current.firma !== firma) solicitud.current = { firma, id: crypto.randomUUID() };
      const r = await adminService.solicitarReposicion({ ...datos, idSolicitud: solicitud.current.id });
      solicitud.current = null;
      setInfo(
        r.estado === "SOLICITADA"
          ? `${codigoCorto(r.id)} guardada. La respuesta del proveedor está pendiente: podés reintentar desde Seguimiento.`
          : r.estado === "ACEPTADA"
            ? `${codigoCorto(r.id)} aceptada por el proveedor. El stock cambiará al registrar la recepción física.`
            : `${codigoCorto(r.id)} rechazada por el proveedor por falta de disponibilidad.`
      );
    });
  };

  const porId = useMemo(() => new Map((productos ?? []).map((p) => [p.id, p])), [productos]);
  const cuenta = (estado) => reposiciones.filter((r) => r.estado === estado).length;
  const activas = cuenta("SOLICITADA") + cuenta("ACEPTADA");
  const recibidas = useMemo(
    () => reposiciones.filter((r) => r.estado === "RECIBIDA").sort((a, b) => new Date(b.recibida) - new Date(a.recibida)),
    [reposiciones]
  );

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return [...reposiciones]
      .sort((a, b) => new Date(b.creada) - new Date(a.creada))
      .filter((r) => filtro === "TODAS" || r.estado === filtro)
      .filter((r) => {
        if (!texto) return true;
        const nombre = porId.get(r.productoId)?.name ?? "";
        return `${codigoCorto(r.id)} ${r.id} ${nombre} ${r.codigoProveedor}`.toLowerCase().includes(texto);
      });
  }, [reposiciones, filtro, busqueda, porId]);

  if (!productos) return <Cargando texto="Cargando reposiciones…" />;

  const elegido = porId.get(Number(productoId));
  const estadoElegido = elegido ? estadoStock(elegido) : null;

  return (
    <>
      <Encabezado
        etiqueta="Gestión de abastecimiento · proveedor mayorista"
        titulo="Reposiciones de Taller"
        descripcion="Pedí mercadería al proveedor externo (servicio SOAP) y registrá la recepción física para acreditar el stock."
        acciones={
          <div className="flex gap-3">
            <div className="rounded-xl bg-surface-container-lowest px-4 py-2 shadow-sm">
              <p className="font-title-md text-title-md">{activas}</p>
              <p className="font-label-sm text-label-sm uppercase text-on-surface-variant">Órdenes activas</p>
            </div>
            <div className="rounded-xl bg-surface-container-lowest px-4 py-2 shadow-sm">
              <p className="font-title-md text-title-md">{recibidas.length}</p>
              <p className="font-label-sm text-label-sm uppercase text-on-surface-variant">Recepciones</p>
            </div>
          </div>
        }
      />

      <nav aria-label="Pasos de reposición" className="mb-8 grid gap-3 md:grid-cols-3">
        {PASOS.map((p) => {
          const activo = p.clave === paso;
          const badge = p.clave === "seguimiento" ? `${activas} activas` : p.clave === "ingresos" ? `${recibidas.length} ingresos` : null;
          return (
            <button key={p.clave} type="button" onClick={() => irA(p.clave)} aria-current={activo ? "step" : undefined}
              className={`flex items-start gap-3 rounded-xl p-5 text-left transition-colors ${
                activo ? "bg-primary text-on-primary shadow-md" : "bg-surface-container-low text-on-surface hover:bg-surface-container"
              }`}>
              <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full font-label-md text-label-md ${
                activo ? "bg-on-primary/20" : "bg-surface-container-highest"}`}>{p.numero}</span>
              <span className="min-w-0 flex-1">
                <span className="block font-headline-sm text-[19px] leading-6">{p.titulo}</span>
                <span className={`block font-body-sm text-body-sm ${activo ? "opacity-80" : "text-on-surface-variant"}`}>{p.detalle}</span>
              </span>
              {badge && (
                <span className={`rounded-full px-2 py-0.5 font-label-sm text-label-sm ${activo ? "bg-on-primary/20" : "bg-surface-container-high"}`}>
                  {badge}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      <Criterio titulo="Criterio operativo de inventario" icono="shield">
        Que el proveedor acepte una orden confirma la fabricación, pero el stock de la tienda se actualiza únicamente al
        confirmar la recepción física en el depósito.
      </Criterio>

      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}
      {info && <Mensaje tono="info" onCerrar={() => setInfo("")}>{info}</Mensaje>}

      {paso === "solicitar" && (
        <Tarjeta className="p-6 md:p-8">
          <h2 className="mb-6 flex items-center gap-2 border-b border-outline-variant/40 pb-4 font-headline-sm text-headline-sm">
            <span className="material-symbols-outlined text-primary">post_add</span>
            Solicitar nueva reposición al proveedor
          </h2>
          <form onSubmit={solicitar} className="grid gap-8 lg:grid-cols-3">
            <div>
              <p className="mb-2 font-label-md text-label-md uppercase tracking-wide">Paso A · Seleccionar joya</p>
              <select required value={productoId} onChange={(e) => setProductoId(e.target.value)} className={campo} disabled={ocupado}>
                <option value="">Elegí una joya del catálogo</option>
                {[...productos].sort((a, b) => (a.stock ?? 0) - (b.stock ?? 0)).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} — stock {p.stock}{p.active === false ? " (archivada)" : ""}
                  </option>
                ))}
              </select>
              {elegido && (
                <div className="mt-3 flex items-center gap-3 rounded-lg bg-surface-container-low p-3">
                  <img src={imagenDeProducto(elegido)} alt="" className="h-12 w-12 rounded object-cover" />
                  <div className="min-w-0">
                    <p className="truncate font-label-md text-label-md">{elegido.name}</p>
                    <p className={`font-body-sm text-body-sm ${estadoElegido.tono === "alerta" ? "text-error" : "text-on-surface-variant"}`}>
                      Stock actual: {elegido.stock} un. · {estadoElegido.texto}
                    </p>
                  </div>
                </div>
              )}
            </div>
            <div>
              <p className="mb-2 font-label-md text-label-md uppercase tracking-wide">Paso B · Código del proveedor</p>
              <div className="mb-3 flex flex-wrap gap-2">
                {CODIGOS_PROVEEDOR.map((c) => (
                  <button key={c} type="button" onClick={() => { setCodigo(c); setDisponibilidad(null); }}
                    className={`rounded-full px-4 py-2 font-label-md text-label-md ${
                      codigo === c ? "bg-primary-container text-on-primary" : "bg-surface-container text-on-surface hover:bg-surface-container-high"
                    }`}>
                    {c}
                  </button>
                ))}
              </div>
              <input value={codigo} onChange={(e) => { setCodigo(e.target.value.toUpperCase()); setDisponibilidad(null); }}
                className={campo} aria-label="Código del proveedor" placeholder="O ingresá otro código mayorista…" disabled={ocupado} />
              {!codigoValido && <p className="mt-1 font-body-sm text-body-sm text-error">Solo mayúsculas, números y guiones.</p>}
            </div>
            <div>
              <p className="mb-2 font-label-md text-label-md uppercase tracking-wide">Paso C · Cantidad a reponer</p>
              <div className="flex items-center gap-3">
                <input type="number" min="1" max="10000" required value={cantidad} onChange={(e) => setCantidad(e.target.value)}
                  className={`${campo} w-32 text-center font-title-md text-title-md`} aria-label="Cantidad" disabled={ocupado} />
                <span>unidades</span>
              </div>
              {!cantidadValida && <p className="mt-1 font-body-sm text-body-sm text-error">Entre 1 y 10.000 unidades.</p>}
            </div>

            <div className="flex flex-col gap-4 border-t border-outline-variant/40 pt-6 lg:col-span-3 lg:flex-row lg:items-center">
              <div className="flex min-h-[3.5rem] flex-1 items-center gap-3 rounded-lg bg-surface-container-low px-4 py-3">
                <span className="material-symbols-outlined text-primary">inventory</span>
                {disponibilidad ? (
                  <p>
                    <strong className="text-primary">{disponibilidad.unidades} unidades disponibles</strong> de {disponibilidad.codigo} para
                    despacho inmediato en el proveedor.
                    {cantidadValida && Number(cantidad) > disponibilidad.unidades && (
                      <span className="block text-error">La cantidad pedida supera la disponibilidad: el proveedor la rechazará.</span>
                    )}
                  </p>
                ) : (
                  <p className="text-on-surface-variant">Consultá la disponibilidad antes de enviar la solicitud.</p>
                )}
              </div>
              <Boton variante="secundario" icono="manage_search" onClick={consultar} disabled={ocupado || !codigoValido}>
                Consultar disponibilidad
              </Boton>
              <Boton type="submit" icono="send" disabled={ocupado || !productoId || !codigoValido || !cantidadValida}>
                {ocupado ? "Enviando…" : "Enviar solicitud de reposición"}
              </Boton>
            </div>
          </form>
        </Tarjeta>
      )}

      {paso === "seguimiento" && (
        <>
          <Tarjeta className="mb-6 flex flex-col gap-4 p-4 lg:flex-row lg:items-center lg:justify-between">
            <Chips etiqueta="Filtrar por estado" valor={filtro} onCambiar={setFiltro} opciones={[
              { valor: "TODAS", texto: "Todas", cantidad: reposiciones.length },
              { valor: "SOLICITADA", texto: "Pendientes", cantidad: cuenta("SOLICITADA") },
              { valor: "ACEPTADA", texto: "Aceptadas", cantidad: cuenta("ACEPTADA") },
              { valor: "RECIBIDA", texto: "Recibidas", cantidad: cuenta("RECIBIDA") },
              { valor: "RECHAZADA", texto: "Rechazadas", cantidad: cuenta("RECHAZADA") },
            ]} />
            <Busqueda valor={busqueda} onCambiar={setBusqueda} placeholder="Buscar por orden o joya…" />
          </Tarjeta>
          {visibles.length === 0 ? (
            <Tarjeta><Vacio icono="local_shipping" titulo="No hay reposiciones para mostrar">Creá una desde el paso 1.</Vacio></Tarjeta>
          ) : (
            <div className="space-y-4">
              {visibles.map((r) => {
                const producto = porId.get(r.productoId);
                const est = ESTADOS_REPOSICION[r.estado] ?? { texto: r.estado, tono: "neutro" };
                const borde = { SOLICITADA: "border-l-outline-variant", ACEPTADA: "border-l-primary-container",
                  RECIBIDA: "border-l-primary", RECHAZADA: "border-l-error" }[r.estado] ?? "border-l-outline-variant";
                return (
                  <Tarjeta key={r.id} className={`flex flex-col gap-5 border-l-4 p-5 md:flex-row md:items-center ${borde}`}>
                    <img src={imagenDeProducto(producto)} alt="" className="h-24 w-24 shrink-0 rounded-lg object-cover" />
                    <div className="min-w-0 flex-1">
                      <p className="flex flex-wrap items-center gap-x-2 font-label-sm text-label-sm uppercase text-on-surface-variant">
                        <span className="text-primary">{codigoCorto(r.id)}</span>· {r.codigoProveedor} · Solicitada {fechaHora(r.creada)}
                      </p>
                      <h3 className="mt-1 font-headline-sm text-headline-sm">{producto?.name ?? `Producto #${r.productoId}`}</h3>
                      <p className="mt-1 text-on-surface-variant">{descripcionReposicion(r)}</p>
                      <div className="mt-3 flex flex-wrap items-center gap-3">
                        <Estado tono={est.tono}>{est.texto}</Estado>
                        <span className="font-label-md text-label-md">{r.cantidad} unidades</span>
                      </div>
                    </div>
                    <div className="shrink-0 md:w-64">
                      {r.estado === "SOLICITADA" && (
                        <Boton variante="secundario" icono="sync" className="w-full" disabled={ocupado}
                          onClick={() => ejecutar(() => adminService.sincronizarReposicion(r.id))}>
                          Reintentar consulta
                        </Boton>
                      )}
                      {r.estado === "ACEPTADA" && (confirmando === r.id ? (
                        <div className="space-y-2 rounded-lg bg-surface-container-low p-3">
                          <p className="font-body-sm text-body-sm">¿Confirmás que la mercadería llegó físicamente al depósito?</p>
                          <div className="flex gap-2">
                            <Boton className="flex-1 px-3" disabled={ocupado}
                              onClick={() => ejecutar(async () => { await adminService.recibirReposicion(r.id); setConfirmando(null); })}>
                              Confirmar
                            </Boton>
                            <Boton variante="secundario" className="px-3" disabled={ocupado} onClick={() => setConfirmando(null)}>
                              Volver
                            </Boton>
                          </div>
                        </div>
                      ) : (
                        <Boton icono="assignment_turned_in" className="w-full" disabled={ocupado} onClick={() => setConfirmando(r.id)}>
                          Registrar recepción física
                        </Boton>
                      ))}
                      {r.estado === "RECIBIDA" && !r.inventarioActualizado && (
                        <p className="flex items-center gap-2 font-body-sm text-body-sm text-on-surface-variant">
                          <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
                          {r.eventoPublicado ? "Acreditando inventario…" : "Publicando evento…"}
                        </p>
                      )}
                    </div>
                  </Tarjeta>
                );
              })}
            </div>
          )}
        </>
      )}

      {paso === "ingresos" && (
        <>
          <div className="mb-8 grid gap-5 md:grid-cols-3">
            <Indicador etiqueta="Unidades ingresadas" icono="trending_up" valor={`+${recibidas.reduce((t, r) => t + r.cantidad, 0)} piezas`}
              detalle="Acreditadas o en proceso de acreditación" />
            <Indicador etiqueta="Recepciones registradas" icono="receipt" tono="calido" valor={recibidas.length}
              detalle={`${recibidas.filter((r) => r.inventarioActualizado).length} con stock acreditado`} />
            <Indicador etiqueta="Pendientes de acreditar" icono="hourglass_top"
              alerta={recibidas.some((r) => !r.inventarioActualizado)}
              valor={recibidas.filter((r) => !r.inventarioActualizado).length} detalle="Eventos en tránsito por la mensajería" />
          </div>
          <Tarjeta className="overflow-hidden">
            {recibidas.length === 0 ? (
              <Vacio icono="inventory_2" titulo="Todavía no hay ingresos">Registrá la recepción de una orden aceptada en Seguimiento.</Vacio>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left">
                  <thead className="bg-surface-container-low font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">
                    <tr>
                      <th className="px-6 py-4">Fecha de recepción</th>
                      <th className="px-4 py-4">Orden</th>
                      <th className="px-4 py-4">Joya</th>
                      <th className="px-4 py-4">Cantidad</th>
                      <th className="px-6 py-4">Inventario</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant/30">
                    {recibidas.map((r) => {
                      const producto = porId.get(r.productoId);
                      return (
                        <tr key={r.id}>
                          <td className="px-6 py-4">{fechaHora(r.recibida)}</td>
                          <td className="px-4 py-4">
                            <p className="font-label-md text-label-md text-primary">{codigoCorto(r.id)}</p>
                            <p className="font-body-sm text-body-sm text-on-surface-variant">{r.codigoProveedor}</p>
                          </td>
                          <td className="px-4 py-4">
                            <div className="flex items-center gap-3">
                              <img src={imagenDeProducto(producto)} alt="" className="h-10 w-10 rounded object-cover" />
                              <span>{producto?.name ?? `Producto #${r.productoId}`}</span>
                            </div>
                          </td>
                          <td className="px-4 py-4">
                            <span className="rounded-full bg-tertiary-fixed px-3 py-1 font-label-md text-label-md text-on-tertiary-fixed-variant">
                              +{r.cantidad} un.
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <Estado tono={r.inventarioActualizado ? "exito" : "pendiente"}>
                              {r.inventarioActualizado ? "Stock acreditado" : "Acreditando…"}
                            </Estado>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Tarjeta>
        </>
      )}
    </>
  );
}
