import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { adminService } from "../../services/adminService";
import { imagenDeProducto } from "../../utils/imagenes";
import {
  ESTADOS_PEDIDO, UMBRAL_STOCK_CRITICO, estadoStock, fecha, fechaHora, moneda,
} from "../../utils/admin";
import {
  Boton, Cargando, Estado, Indicador, Mensaje, Tarjeta, TituloSeccion, Vacio, campo,
} from "../../components/admin/AdminUI";

const enlace = "inline-flex items-center gap-1 font-label-md text-label-md text-primary hover:underline";

export default function AdminPanel() {
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [datos, setDatos] = useState(null);
  const [errores, setErrores] = useState([]);
  const [umbral, setUmbral] = useState(UMBRAL_STOCK_CRITICO);
  const [actualizando, setActualizando] = useState(false);

  const cargar = useCallback(async () => {
    setActualizando(true);
    const fuentes = ["productos", "pedidos", "reposiciones", "avisos"];
    const resultados = await Promise.allSettled(fuentes.map((f) => adminService[f]()));
    const nuevos = {};
    const fallas = [];
    resultados.forEach((r, i) => {
      nuevos[fuentes[i]] = r.status === "fulfilled" ? r.value ?? [] : [];
      if (r.status === "rejected") fallas.push(`${fuentes[i]}: ${r.reason?.message}`);
    });
    setDatos(nuevos);
    setErrores(fallas);
    setActualizando(false);
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const resumen = useMemo(() => {
    if (!datos) return null;
    const activos = datos.productos.filter((p) => p.active !== false);
    const criticos = activos
      .filter((p) => (Number(p.stock) || 0) <= umbral)
      .sort((a, b) => (a.stock ?? 0) - (b.stock ?? 0));
    const hoy = new Date();
    const delMes = datos.pedidos.filter((o) => {
      const f = new Date(`${o.orderDate}T00:00:00`);
      return f.getMonth() === hoy.getMonth() && f.getFullYear() === hoy.getFullYear();
    });
    const vendidos = delMes.filter((o) => o.status !== "CANCELLED");
    const enCurso = datos.reposiciones.filter((r) => r.estado === "SOLICITADA" || r.estado === "ACEPTADA");
    const avisos = [...datos.avisos].sort((a, b) => new Date(b.creada) - new Date(a.creada));
    const pedidos = [...datos.pedidos].sort((a, b) => b.id - a.id);
    return {
      criticos,
      agotados: criticos.filter((p) => (Number(p.stock) || 0) === 0).length,
      ventasMes: vendidos.reduce((t, o) => t + (o.total ?? 0), 0),
      pedidosMes: vendidos.length,
      pendientes: datos.pedidos.filter((o) => o.status === "PENDING" || o.status === "CONFIRMED").length,
      enCurso,
      aceptadas: enCurso.filter((r) => r.estado === "ACEPTADA").length,
      avisos,
      pedidos,
    };
  }, [datos, umbral]);

  if (!resumen) return <Cargando texto="Cargando el resumen del atelier…" />;

  return (
    <>
      <Tarjeta className="mb-8 flex flex-col gap-6 p-6 md:p-8 lg:flex-row lg:items-center lg:justify-between">
        <div className="max-w-2xl">
          <p className="mb-2 font-label-sm text-label-sm uppercase tracking-[0.18em] text-primary">
            Resumen gerencial del atelier
          </p>
          <h1 className="font-headline-lg text-headline-lg-mobile md:text-headline-lg">Panel de Control General</h1>
          <p className="mt-2 font-body-lg text-body-lg text-on-surface-variant">
            Bienvenida/o, {usuario?.nombre}. Acá tenés el resumen de ventas, existencias y reposiciones a proveedores.
          </p>
        </div>
        <div className="flex flex-col gap-3 rounded-xl bg-surface-container-low p-5">
          <label className="flex items-center gap-3 font-label-md text-label-md">
            <span className="material-symbols-outlined text-primary">tune</span>
            Alerta de stock bajo: ≤
            <input
              type="number"
              min="0"
              max="999"
              value={umbral}
              onChange={(e) => setUmbral(Math.max(0, Number(e.target.value) || 0))}
              className={`${campo} w-20`}
              aria-label="Umbral de stock bajo"
            />
            un.
          </label>
          <Boton icono="sync" onClick={cargar} disabled={actualizando}>
            {actualizando ? "Actualizando…" : "Actualizar datos"}
          </Boton>
        </div>
      </Tarjeta>

      {errores.length > 0 && (
        <Mensaje>No se pudo cargar parte del resumen ({errores.join(" · ")}).</Mensaje>
      )}

      <div className="mb-8 grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
        <Indicador
          etiqueta="Inventario en riesgo"
          icono="warning"
          tono="alerta"
          alerta={resumen.criticos.length > 0}
          valor={`${resumen.criticos.length} ${resumen.criticos.length === 1 ? "pieza" : "piezas"} en alerta`}
          detalle={`${resumen.agotados} agotadas · ${resumen.criticos.length - resumen.agotados} por agotarse`}
          accion={<Link to="/admin/productos?filtro=critico" className={enlace}>Ver inventario crítico →</Link>}
        />
        <Indicador
          etiqueta="Ventas del mes"
          icono="payments"
          tono="calido"
          valor={moneda(resumen.ventasMes)}
          detalle={`${resumen.pedidosMes} pedidos este mes · ${resumen.pendientes} en curso`}
          accion={<Link to="/admin/pedidos" className={enlace}>Ver pedidos →</Link>}
        />
        <Indicador
          etiqueta="Reposiciones en curso"
          icono="precision_manufacturing"
          tono="calido"
          valor={`${resumen.enCurso.length} ${resumen.enCurso.length === 1 ? "orden activa" : "órdenes activas"}`}
          detalle={`${resumen.aceptadas} aceptadas · ${resumen.enCurso.length - resumen.aceptadas} esperando respuesta`}
          accion={<Link to="/admin/reposiciones?paso=seguimiento" className={enlace}>Ver reposiciones →</Link>}
        />
        <Indicador
          etiqueta="Avisos administrativos"
          icono="notifications"
          valor={`${resumen.avisos.length} ${resumen.avisos.length === 1 ? "aviso" : "avisos"}`}
          detalle={resumen.avisos[0] ? `Último: ${fechaHora(resumen.avisos[0].creada)}` : "Sin avisos todavía"}
          accion={<Link to="/admin/avisos" className={enlace}>Ver avisos →</Link>}
        />
      </div>

      <div className="mb-8 grid gap-6 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Tarjeta className="p-6">
          <TituloSeccion etiqueta="Atención prioritaria" tonoEtiqueta="text-error" titulo="Piezas en stock crítico" />
          {resumen.criticos.length === 0 ? (
            <Vacio icono="verified" titulo="Sin piezas en alerta">Todas las joyas activas superan el umbral elegido.</Vacio>
          ) : (
            <ul className="space-y-3">
              {resumen.criticos.slice(0, 5).map((p) => {
                const estado = estadoStock(p, umbral);
                return (
                  <li key={p.id} className="flex flex-col gap-4 rounded-lg bg-surface-container-low p-4 sm:flex-row sm:items-center">
                    <img src={imagenDeProducto(p)} alt="" className="h-16 w-16 shrink-0 rounded object-cover" />
                    <div className="min-w-0 flex-1">
                      <div className="mb-1 flex flex-wrap items-center gap-2">
                        <span className="font-label-sm text-label-sm uppercase text-on-surface-variant">
                          {p.category?.description || "Sin categoría"}
                        </span>
                        <Estado tono={estado.tono}>{estado.texto}</Estado>
                      </div>
                      <p className="truncate font-title-md text-title-md">{p.name}</p>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">
                        Depósito: {p.stock} {p.stock === 1 ? "unidad" : "unidades"}
                      </p>
                    </div>
                    <Boton icono="local_shipping" onClick={() => navigate(`/admin/reposiciones?producto=${p.id}`)}>
                      Solicitar reposición
                    </Boton>
                  </li>
                );
              })}
            </ul>
          )}
          <div className="mt-5 text-right">
            <Link to="/admin/productos" className={enlace}>Administrar todo el inventario →</Link>
          </div>
        </Tarjeta>

        <Tarjeta className="p-6">
          <TituloSeccion
            etiqueta="Flujo de venta"
            titulo="Últimos pedidos"
            accion={<Link to="/admin/pedidos" className={enlace}>Ver todos</Link>}
          />
          {resumen.pedidos.length === 0 ? (
            <Vacio icono="receipt_long" titulo="Todavía no hay pedidos" />
          ) : (
            <ul className="space-y-3">
              {resumen.pedidos.slice(0, 3).map((o) => {
                const estado = ESTADOS_PEDIDO[o.status] ?? { texto: o.status, tono: "neutro" };
                const unidades = (o.items ?? []).reduce((t, i) => t + (i.quantity ?? 0), 0);
                return (
                  <li key={o.id} className="rounded-lg bg-surface-container-low p-4">
                    <div className="mb-2 flex items-center justify-between gap-3">
                      <span className="font-label-md text-label-md">#ORD-{o.id}</span>
                      <Estado tono={estado.tono}>{estado.texto}</Estado>
                    </div>
                    <div className="flex items-baseline justify-between gap-3">
                      <span className="truncate">{o.user ? `${o.user.nombre} ${o.user.apellido}` : "Cliente"}</span>
                      <span className="font-title-md text-title-md text-primary">{moneda(o.total)}</span>
                    </div>
                    <p className="mt-1 font-body-sm text-body-sm text-on-surface-variant">
                      {fecha(o.orderDate)} · {unidades} {unidades === 1 ? "artículo" : "artículos"} · {o.shippingAddress}
                    </p>
                  </li>
                );
              })}
            </ul>
          )}
        </Tarjeta>
      </div>

      <Tarjeta className="p-6 md:p-8">
        <TituloSeccion
          etiqueta="Registro de operaciones"
          titulo="Últimos avisos de recepción"
          accion={<Link to="/admin/avisos" className={enlace}>Ver todos</Link>}
        />
        {resumen.avisos.length === 0 ? (
          <Vacio icono="notifications" titulo="Sin avisos">
            Los avisos se generan cuando se registra la recepción de una reposición.
          </Vacio>
        ) : (
          <ol className="space-y-5 border-l border-outline-variant/50 pl-6">
            {resumen.avisos.slice(0, 3).map((n) => (
              <li key={n.id} className="relative">
                <span className="absolute -left-[31px] top-1.5 h-3 w-3 rounded-full bg-primary" aria-hidden="true" />
                <p className="font-label-sm text-label-sm uppercase text-primary">{fechaHora(n.creada)}</p>
                <p className="mt-1">{n.mensaje}</p>
              </li>
            ))}
          </ol>
        )}
      </Tarjeta>
    </>
  );
}
