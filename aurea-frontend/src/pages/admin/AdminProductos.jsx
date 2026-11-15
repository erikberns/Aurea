import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { adminService } from "../../services/adminService";
import { imagenDeProducto } from "../../utils/imagenes";
import { estadoStock, moneda } from "../../utils/admin";
import {
  Boton, Busqueda, Cargando, Chips, Encabezado, Estado, Indicador, Mensaje, Tarjeta, Vacio, campo,
} from "../../components/admin/AdminUI";

const POR_PAGINA = 8;
const FILTROS = [
  { valor: "todos", texto: "Todas" },
  { valor: "disponible", texto: "En stock" },
  { valor: "critico", texto: "Crítico (≤ 5)" },
  { valor: "agotado", texto: "Agotadas" },
  { valor: "archivado", texto: "Archivadas" },
];

/** Edición en línea de precio o stock: un PATCH por campo, igual que la API REST. */
function CeldaEditable({ valor, formato, tipo, onGuardar, etiqueta, deshabilitado }) {
  const [editando, setEditando] = useState(false);
  const [borrador, setBorrador] = useState(String(valor ?? ""));
  const [guardando, setGuardando] = useState(false);

  const guardar = async () => {
    const numero = Number(borrador);
    const valido = tipo === "stock" ? Number.isInteger(numero) && numero >= 0 : Number.isFinite(numero) && numero > 0;
    if (!valido || borrador.trim() === "") return;
    setGuardando(true);
    const ok = await onGuardar(numero);
    setGuardando(false);
    if (ok) setEditando(false);
  };

  if (!editando) {
    return (
      <button
        type="button"
        disabled={deshabilitado}
        onClick={() => {
          setBorrador(String(valor ?? ""));
          setEditando(true);
        }}
        className="group inline-flex items-center gap-1.5 rounded px-1 py-0.5 hover:bg-surface-container disabled:cursor-default disabled:hover:bg-transparent"
        aria-label={`Editar ${etiqueta}`}
      >
        {formato(valor)}
        {!deshabilitado && (
          <span className="material-symbols-outlined text-[16px] text-outline group-hover:text-primary">edit</span>
        )}
      </button>
    );
  }
  return (
    <form
      className="flex items-center gap-1"
      onSubmit={(e) => {
        e.preventDefault();
        guardar();
      }}
    >
      <input
        autoFocus
        type="number"
        min={tipo === "stock" ? 0 : 1}
        step={tipo === "stock" ? 1 : "any"}
        value={borrador}
        onChange={(e) => setBorrador(e.target.value)}
        onKeyDown={(e) => e.key === "Escape" && setEditando(false)}
        className={`${campo} h-9 w-24 px-2`}
        aria-label={etiqueta}
        disabled={guardando}
      />
      <button type="submit" disabled={guardando} aria-label="Guardar" className="rounded p-1 text-primary hover:bg-surface-container">
        <span className="material-symbols-outlined text-[18px]">check</span>
      </button>
      <button type="button" onClick={() => setEditando(false)} aria-label="Cancelar" className="rounded p-1 text-outline hover:bg-surface-container">
        <span className="material-symbols-outlined text-[18px]">close</span>
      </button>
    </form>
  );
}

export default function AdminProductos() {
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const [productos, setProductos] = useState(null);
  const [categorias, setCategorias] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [categoria, setCategoria] = useState("");
  const [pagina, setPagina] = useState(1);
  const [error, setError] = useState("");
  const [exito, setExito] = useState(location.state?.mensaje ?? "");
  const filtro = FILTROS.some((f) => f.valor === params.get("filtro")) ? params.get("filtro") : "todos";

  const cargar = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([adminService.productos(), adminService.categorias()]);
      setProductos(p);
      setCategorias(c);
    } catch (e) {
      setError(e.message);
      setProductos((actual) => actual ?? []);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const reemplazar = (actualizado) =>
    setProductos((lista) => lista.map((p) => (p.id === actualizado.id ? actualizado : p)));

  const ejecutar = async (accion, mensaje) => {
    setError("");
    setExito("");
    try {
      reemplazar(await accion());
      setExito(mensaje);
      return true;
    } catch (e) {
      setError(e.message);
      return false;
    }
  };

  const archivar = (p) => {
    const activar = p.active === false;
    const confirmado = window.confirm(
      activar
        ? `¿Volver a publicar "${p.name}" en la tienda?`
        : `¿Archivar "${p.name}"? Dejará de mostrarse en la tienda, pero se conserva en el historial de pedidos.`
    );
    if (!confirmado) return;
    ejecutar(
      () => adminService.cambiarEstado(p.id, activar),
      activar ? `"${p.name}" volvió a la tienda.` : `"${p.name}" quedó archivada.`
    );
  };

  const resumen = useMemo(() => {
    const lista = productos ?? [];
    const activos = lista.filter((p) => p.active !== false);
    return {
      activos: activos.length,
      criticos: activos.filter((p) => estadoStock(p).clave === "critico").length,
      agotados: activos.filter((p) => estadoStock(p).clave === "agotado").length,
      valuacion: activos.reduce((t, p) => t + (p.price ?? 0) * (p.stock ?? 0), 0),
      archivados: lista.length - activos.length,
    };
  }, [productos]);

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return (productos ?? [])
      .filter((p) => {
        const clave = estadoStock(p).clave;
        if (filtro === "todos") return clave !== "archivado";
        if (filtro === "disponible") return clave === "disponible";
        return clave === filtro;
      })
      .filter((p) => !categoria || String(p.category?.id) === categoria)
      .filter((p) => !texto || `${p.name} ${p.description ?? ""}`.toLowerCase().includes(texto))
      .sort((a, b) => a.name.localeCompare(b.name, "es"));
  }, [productos, filtro, categoria, busqueda]);

  const paginas = Math.max(1, Math.ceil(visibles.length / POR_PAGINA));
  const paginaActual = Math.min(pagina, paginas);
  const pagina_ = visibles.slice((paginaActual - 1) * POR_PAGINA, paginaActual * POR_PAGINA);

  const cambiarFiltro = (valor) => {
    setPagina(1);
    setParams(valor === "todos" ? {} : { filtro: valor }, { replace: true });
  };

  if (!productos) return <Cargando texto="Cargando catálogo…" />;

  return (
    <>
      <Encabezado
        etiqueta="Control de taller & orfebrería"
        titulo="Catálogo de Joyas e Inventario"
        descripcion="Administrá el catálogo, actualizá precios y existencias en depósito, o incorporá nuevas joyas."
        acciones={
          <Boton icono="add" onClick={() => navigate("/admin/productos/nueva")}>
            Nueva joya
          </Boton>
        }
      />

      {exito && <Mensaje tono="exito" onCerrar={() => setExito("")}>{exito}</Mensaje>}
      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}

      <div className="mb-8 grid gap-5 md:grid-cols-3">
        <Indicador etiqueta="Joyas en catálogo" icono="diamond" valor={`${resumen.activos} piezas`}
          detalle={`En exhibición · ${resumen.archivados} archivadas`} />
        <Indicador etiqueta="Atención de taller" icono="warning" tono="alerta" alerta={resumen.criticos + resumen.agotados > 0}
          valor={`${resumen.criticos} críticas`} detalle={`Stock ≤ 5 un. · ${resumen.agotados} agotadas`} />
        <Indicador etiqueta="Valuación en depósito" icono="account_balance_wallet" tono="calido"
          valor={moneda(resumen.valuacion)} detalle="Precio público × stock, en pesos argentinos (ARS)" />
      </div>

      <Tarjeta className="mb-6 flex flex-col gap-4 p-4 lg:flex-row lg:items-center">
        <Busqueda valor={busqueda} onCambiar={(v) => { setBusqueda(v); setPagina(1); }} placeholder="Buscá por nombre o descripción" />
        <label className="w-full lg:w-56">
          <span className="sr-only">Categoría</span>
          <select value={categoria} onChange={(e) => { setCategoria(e.target.value); setPagina(1); }} className={campo}>
            <option value="">Todas las categorías</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.description}</option>)}
          </select>
        </label>
        <div className="lg:ml-auto">
          <Chips etiqueta="Filtrar por estado" opciones={FILTROS} valor={filtro} onCambiar={cambiarFiltro} />
        </div>
      </Tarjeta>

      <Tarjeta className="overflow-hidden">
        {visibles.length === 0 ? (
          <Vacio icono="search_off" titulo="No hay joyas para mostrar">Probá con otro filtro o búsqueda.</Vacio>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left">
              <thead className="bg-surface-container-low font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">
                <tr>
                  <th className="px-6 py-4">Pieza de joyería</th>
                  <th className="px-4 py-4">Categoría</th>
                  <th className="px-4 py-4">Precio público</th>
                  <th className="px-4 py-4">Stock depósito</th>
                  <th className="px-4 py-4">Estado</th>
                  <th className="px-6 py-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30">
                {pagina_.map((p) => {
                  const estado = estadoStock(p);
                  const archivada = p.active === false;
                  return (
                    <tr key={p.id} className={archivada ? "opacity-60" : ""}>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-4">
                          <img src={imagenDeProducto(p)} alt="" className="h-14 w-14 shrink-0 rounded object-cover" />
                          <div className="min-w-0">
                            <Link to={`/admin/productos/${p.id}`} className="font-title-md text-title-md hover:text-primary">{p.name}</Link>
                            <p className="line-clamp-1 max-w-xs font-body-sm text-body-sm text-on-surface-variant">{p.description}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <span className="rounded bg-surface-container px-2 py-1 font-label-sm text-label-sm uppercase">
                          {p.category?.description ?? "Sin categoría"}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <CeldaEditable valor={p.price} formato={moneda} tipo="precio" etiqueta={`precio de ${p.name}`}
                          deshabilitado={archivada}
                          onGuardar={(v) => ejecutar(() => adminService.actualizarPrecio(p.id, v), `Precio de "${p.name}" actualizado.`)} />
                      </td>
                      <td className={`px-4 py-4 ${estado.clave === "critico" ? "text-error" : ""}`}>
                        <CeldaEditable valor={p.stock} formato={(v) => `${v ?? 0} un.`} tipo="stock" etiqueta={`stock de ${p.name}`}
                          deshabilitado={archivada}
                          onGuardar={(v) => ejecutar(() => adminService.actualizarStock(p.id, v), `Stock de "${p.name}" actualizado.`)} />
                      </td>
                      <td className="px-4 py-4"><Estado tono={estado.tono}>{estado.texto}</Estado></td>
                      <td className="px-6 py-4">
                        <div className="flex justify-end gap-1">
                          <Link to={`/admin/productos/${p.id}`} aria-label={`Editar ${p.name}`} title="Editar ficha"
                            className="rounded p-2 text-on-surface-variant hover:bg-surface-container hover:text-primary">
                            <span className="material-symbols-outlined">edit_note</span>
                          </Link>
                          {!archivada && (
                            <Link to={`/admin/reposiciones?producto=${p.id}`} aria-label={`Reponer ${p.name}`} title="Solicitar reposición"
                              className="rounded p-2 text-on-surface-variant hover:bg-surface-container hover:text-primary">
                              <span className="material-symbols-outlined">local_shipping</span>
                            </Link>
                          )}
                          <button type="button" onClick={() => archivar(p)} title={archivada ? "Volver a publicar" : "Archivar"}
                            aria-label={`${archivada ? "Reactivar" : "Archivar"} ${p.name}`}
                            className="rounded p-2 text-on-surface-variant hover:bg-surface-container hover:text-primary">
                            <span className="material-symbols-outlined">{archivada ? "unarchive" : "archive"}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <div className="flex flex-col items-center justify-between gap-3 border-t border-outline-variant/30 bg-surface-container-low px-6 py-4 sm:flex-row">
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Mostrando {pagina_.length} de {visibles.length} piezas
          </p>
          <nav aria-label="Paginación" className="flex items-center gap-1">
            <button type="button" disabled={paginaActual === 1} onClick={() => setPagina(paginaActual - 1)} aria-label="Página anterior"
              className="rounded p-2 hover:bg-surface-container disabled:opacity-40">
              <span className="material-symbols-outlined">chevron_left</span>
            </button>
            {Array.from({ length: paginas }, (_, i) => i + 1).map((n) => (
              <button key={n} type="button" onClick={() => setPagina(n)} aria-current={n === paginaActual ? "page" : undefined}
                className={`h-9 min-w-9 rounded px-3 font-label-md text-label-md ${n === paginaActual ? "bg-primary-container text-on-primary" : "hover:bg-surface-container"}`}>
                {n}
              </button>
            ))}
            <button type="button" disabled={paginaActual === paginas} onClick={() => setPagina(paginaActual + 1)} aria-label="Página siguiente"
              className="rounded p-2 hover:bg-surface-container disabled:opacity-40">
              <span className="material-symbols-outlined">chevron_right</span>
            </button>
          </nav>
        </div>
      </Tarjeta>
    </>
  );
}
