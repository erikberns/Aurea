import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { catalogoService } from "../services/catalogoService";
import { useCart } from "../context/CartContext";

const fmt = new Intl.NumberFormat("es-AR");

export default function ProductoDetalle() {
  const { id } = useParams();
  const [joya, setJoya] = useState(null);
  const [opcion, setOpcion] = useState(null);
  const { agregarItem } = useCart();
  const [agregado, setAgregado] = useState(false);

  useEffect(() => {
    catalogoService.consultarJoya(id).then((j) => {
      setJoya(j);
      setOpcion(j?.opciones?.[0]);
    });
  }, [id]);

  if (!joya) {
    return <div className="px-6 lg:px-16 py-16 text-center text-on-surface-variant">Cargando joya…</div>;
  }

  return (
    <div className="w-full px-6 lg:px-16 mx-auto py-10">
      <Link to="/catalogo" className="font-label-md text-label-md text-secondary uppercase tracking-wide">
        ← Volver al catálogo
      </Link>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mt-6">
        <div className="aspect-[4/5] bg-surface-container rounded overflow-hidden">
          <img src={joya.imagen} alt={joya.nombre} className="w-full h-full object-cover" />
        </div>
        <div>
          <span className="font-label-sm text-label-sm uppercase tracking-wider text-secondary">
            {joya.categoria}
          </span>
          <h1 className="font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface mt-2">
            {joya.nombre}
          </h1>
          <p className="font-body-lg text-body-lg text-on-surface-variant mt-3">{joya.descripcion}</p>
          <p className="font-title-md text-title-md text-on-surface mt-6">${fmt.format(joya.precio)}</p>

          {joya.opciones?.length > 0 && (
            <div className="mt-6">
              <span className="font-label-md text-label-md uppercase tracking-wide text-on-surface-variant">
                Opción
              </span>
              <div className="flex flex-wrap gap-2 mt-2">
                {joya.opciones.map((op) => (
                  <button
                    key={op}
                    onClick={() => setOpcion(op)}
                    className={`px-4 py-2 rounded-full border font-body-sm text-body-sm transition-colors ${
                      opcion === op
                        ? "bg-primary-container text-on-primary border-primary-container"
                        : "border-outline-variant text-on-surface-variant hover:border-primary"
                    }`}
                  >
                    {op}
                  </button>
                ))}
              </div>
            </div>
          )}

          <button
            onClick={() => {
              agregarItem(joya, opcion);
              setAgregado(true);
              setTimeout(() => setAgregado(false), 1800);
            }}
            className="mt-8 w-full sm:w-auto flex items-center justify-center gap-2 bg-[#1e1b18] text-[#fff8f4] font-label-md text-label-md uppercase tracking-wider px-6 py-3.5 rounded-sm hover:bg-primary transition-colors"
          >
            <span className="material-symbols-outlined text-base">shopping_bag</span>
            {agregado ? "Añadido a la bolsa ✓" : "Añadir a la Bolsa"}
          </button>

          <ul className="mt-8 space-y-2 font-body-sm text-body-sm text-on-surface-variant">
            <li>✓ Garantía oficial de 2 años</li>
            <li>✓ Packaging de regalo sostenible incluido</li>
            <li>✓ Envío asegurado y con seguimiento</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
