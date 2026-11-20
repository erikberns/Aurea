// Piezas visuales del panel de administración (estética de las maquetas de Áurea).
import AureaLoader from "../AureaLoader";

export const campo =
  "w-full h-11 rounded border border-outline-variant/70 bg-surface-container-lowest px-3 font-body-md text-body-md text-on-surface placeholder:text-outline/70 focus:outline-none focus:border-primary focus:ring-2 focus:ring-tertiary/20 disabled:opacity-60";

export function Encabezado({ etiqueta, titulo, descripcion, acciones }) {
  return (
    <div className="mb-8 flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
      <div className="max-w-3xl">
        {etiqueta && (
          <p className="mb-2 font-label-sm text-label-sm uppercase tracking-[0.18em] text-primary">{etiqueta}</p>
        )}
        <h1 className="font-headline-lg text-headline-lg-mobile text-on-surface md:text-headline-lg">{titulo}</h1>
        {descripcion && <p className="mt-2 font-body-lg text-body-lg text-on-surface-variant">{descripcion}</p>}
      </div>
      {acciones && <div className="flex shrink-0 flex-wrap gap-3">{acciones}</div>}
    </div>
  );
}

export function Tarjeta({ children, className = "" }) {
  return (
    <section
      className={`rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-[0_8px_30px_-18px_rgba(30,27,24,0.25)] ${className}`}
    >
      {children}
    </section>
  );
}

export function TituloSeccion({ etiqueta, titulo, accion, tonoEtiqueta = "text-primary" }) {
  return (
    <div className="mb-5 flex items-start justify-between gap-4">
      <div>
        {etiqueta && <p className={`font-label-sm text-label-sm uppercase tracking-[0.14em] ${tonoEtiqueta}`}>{etiqueta}</p>}
        <h2 className="mt-1 font-headline-sm text-headline-sm text-on-surface">{titulo}</h2>
      </div>
      {accion}
    </div>
  );
}

const TONOS_ICONO = {
  neutro: "bg-surface-container text-primary",
  alerta: "bg-error-container text-error",
  calido: "bg-secondary-fixed text-secondary",
};

export function Indicador({ etiqueta, valor, detalle, icono, tono = "neutro", accion, alerta = false }) {
  return (
    <Tarjeta className="flex flex-col p-6">
      <div className="flex items-start justify-between gap-3">
        <p className="font-label-sm text-label-sm uppercase tracking-[0.1em] text-on-surface-variant">{etiqueta}</p>
        {icono && (
          <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${TONOS_ICONO[tono]}`}>
            <span className="material-symbols-outlined">{icono}</span>
          </span>
        )}
      </div>
      <p className={`mt-3 font-headline-md text-headline-md ${alerta ? "text-error" : "text-on-surface"}`}>{valor}</p>
      {detalle && <p className={`mt-1 font-body-sm text-body-sm ${alerta ? "text-error" : "text-on-surface-variant"}`}>{detalle}</p>}
      {accion && <div className="mt-auto pt-5">{accion}</div>}
    </Tarjeta>
  );
}

const TONOS_ESTADO = {
  exito: "bg-primary-fixed/70 text-on-primary-fixed-variant",
  alerta: "bg-error-container text-on-error-container",
  pendiente: "bg-tertiary-fixed text-on-tertiary-fixed-variant",
  info: "bg-secondary-fixed text-on-secondary-fixed-variant",
  neutro: "bg-surface-container-high text-on-surface-variant",
};

export function Estado({ tono = "neutro", children }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 font-label-sm text-label-sm uppercase ${TONOS_ESTADO[tono]}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
      {children}
    </span>
  );
}

export function Boton({ variante = "primario", icono, children, className = "", ...props }) {
  const estilos = {
    primario: "bg-primary-container text-on-primary hover:bg-secondary shadow-sm",
    secundario: "bg-surface-container text-primary hover:bg-surface-container-high",
    contorno: "border border-primary-container text-secondary hover:bg-primary-container/10",
    peligro: "border border-error/40 text-error hover:bg-error-container/60",
  };
  return (
    <button
      type="button"
      {...props}
      className={`inline-flex h-11 items-center justify-center gap-2 rounded px-5 font-label-md text-label-md uppercase tracking-wider transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${estilos[variante]} ${className}`}
    >
      {icono && <span className="material-symbols-outlined text-[18px]">{icono}</span>}
      {children}
    </button>
  );
}

export function Mensaje({ tono = "error", children, onCerrar }) {
  const tonos = {
    error: "border-error/30 bg-error-container text-on-error-container",
    exito: "border-primary/20 bg-primary-fixed/50 text-on-primary-fixed-variant",
    info: "border-outline-variant/40 bg-surface-container text-on-surface",
  };
  return (
    <div
      role={tono === "error" ? "alert" : "status"}
      className={`mb-6 flex items-start justify-between gap-4 rounded-lg border px-4 py-3 font-body-md text-body-md ${tonos[tono]}`}
    >
      <div>{children}</div>
      {onCerrar && (
        <button type="button" onClick={onCerrar} aria-label="Cerrar mensaje" className="opacity-70 hover:opacity-100">
          <span className="material-symbols-outlined text-[18px]">close</span>
        </button>
      )}
    </div>
  );
}

export function Criterio({ titulo, children, icono = "info" }) {
  return (
    <div className="mb-8 flex gap-4 rounded-xl border-l-4 border-primary-container bg-surface-container-low p-5">
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-surface-container text-primary">
        <span className="material-symbols-outlined">{icono}</span>
      </span>
      <div>
        <p className="font-label-md text-label-md uppercase tracking-wide text-primary">{titulo}</p>
        <p className="mt-1 font-body-md text-body-md text-on-surface">{children}</p>
      </div>
    </div>
  );
}

export function Vacio({ icono = "inbox", titulo, children }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      <span className="mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container text-primary">
        <span className="material-symbols-outlined">{icono}</span>
      </span>
      <p className="font-title-md text-title-md text-on-surface">{titulo}</p>
      {children && <p className="mt-1 max-w-md font-body-md text-body-md text-on-surface-variant">{children}</p>}
    </div>
  );
}

export function Cargando({ texto = "Cargando…" }) {
  return <AureaLoader text={texto} />;
}

export function Chips({ opciones, valor, onCambiar, etiqueta }) {
  return (
    <div role="tablist" aria-label={etiqueta} className="flex flex-wrap gap-2">
      {opciones.map((op) => (
        <button
          key={op.valor}
          type="button"
          role="tab"
          aria-selected={valor === op.valor}
          onClick={() => onCambiar(op.valor)}
          className={`inline-flex items-center gap-2 rounded-full px-4 py-2 font-label-md text-label-md transition-colors ${
            valor === op.valor
              ? "bg-primary text-on-primary"
              : "border border-outline-variant/50 bg-surface-container-lowest text-on-surface hover:bg-surface-container"
          }`}
        >
          {op.texto}
          {op.cantidad !== undefined && (
            <span
              className={`rounded-full px-2 text-[11px] ${valor === op.valor ? "bg-on-primary/20" : "bg-surface-container"}`}
            >
              {op.cantidad}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}

export function Busqueda({ valor, onCambiar, placeholder }) {
  return (
    <label className="relative block w-full md:max-w-sm">
      <span className="sr-only">{placeholder}</span>
      <span className="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-outline">
        search
      </span>
      <input
        type="search"
        value={valor}
        onChange={(e) => onCambiar(e.target.value)}
        placeholder={placeholder}
        className={`${campo} pl-10`}
      />
    </label>
  );
}
