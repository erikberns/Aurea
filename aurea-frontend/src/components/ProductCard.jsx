import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";

const fmt = new Intl.NumberFormat("es-AR");

export default function ProductCard({ joya }) {
  const { agregarItem } = useCart();

  return (
    <div className="group bg-surface-container-lowest border border-[rgba(140,101,65,0.12)] rounded overflow-hidden flex flex-col">
      <div className="relative aspect-[4/5] overflow-hidden bg-surface-container">
        <span className="absolute top-3 left-3 z-10 bg-[#1e1b18] text-[#fff8f4] font-label-sm text-label-sm uppercase tracking-wider px-2.5 py-1 rounded-sm">
          {joya.badge}
        </span>
        <button
          aria-label="Agregar a favoritos"
          className="absolute top-3 right-3 z-10 bg-surface-container-lowest/90 backdrop-blur rounded-full p-1.5 text-on-surface-variant hover:text-error transition-colors"
        >
          <span className="material-symbols-outlined text-lg">favorite_border</span>
        </button>
        <Link to={`/productos/${joya.id}`}>
          <img
            src={joya.imagen}
            alt={joya.nombre}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        </Link>
      </div>
      <div className="p-4 flex flex-col flex-1">
        <span className="font-label-sm text-label-sm uppercase tracking-wider text-secondary">
          {joya.categoria}
        </span>
        <Link to={`/productos/${joya.id}`}>
          <h3 className="font-headline-sm text-[17px] leading-snug text-on-surface mt-1 hover:text-primary transition-colors">
            {joya.nombre}
          </h3>
        </Link>
        <p className="font-body-sm text-body-sm text-on-surface-variant mt-1.5">{joya.descripcion}</p>
        {joya.opciones?.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            {joya.opciones.map((op) => (
              <span
                key={op}
                className="font-body-sm text-body-sm border border-outline-variant rounded-full px-2.5 py-0.5 text-on-surface-variant"
              >
                {op}
              </span>
            ))}
          </div>
        )}
        <div className="mt-auto pt-4 flex items-center justify-between">
          <div>
            <span className="block font-label-sm text-label-sm text-on-surface-variant uppercase">Precio</span>
            <span className="font-title-md text-title-md text-on-surface">${fmt.format(joya.precio)}</span>
          </div>
          <button
            onClick={() => agregarItem(joya, joya.opciones?.[0])}
            className="flex items-center gap-1.5 bg-[#1e1b18] text-[#fff8f4] font-label-md text-label-md uppercase tracking-wider px-3.5 py-2.5 rounded-sm hover:bg-primary transition-colors"
          >
            <span className="material-symbols-outlined text-base">shopping_bag</span>
            Añadir
          </button>
        </div>
      </div>
    </div>
  );
}
