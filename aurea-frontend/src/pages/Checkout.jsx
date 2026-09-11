import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { PrimaryButton } from "../components/ui";

const fmt = new Intl.NumberFormat("es-AR");

export default function Checkout() {
  const { items, quitarItem, total } = useCart();
  const { estaAutenticado } = useAuth();

  return (
    <div className="w-full px-6 lg:px-16 mx-auto py-10">
      <h1 className="font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface mb-8">
        Tu Bolsa
      </h1>

      {items.length === 0 ? (
        <div className="text-center py-20">
          <p className="font-body-lg text-body-lg text-on-surface-variant mb-6">Tu bolsa está vacía por ahora.</p>
          <Link to="/catalogo" className="text-primary font-label-md text-label-md uppercase tracking-wide hover:underline">
            Ver el catálogo →
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          <div className="lg:col-span-8 space-y-4">
            {items.map((item) => (
              <div
                key={`${item.id}-${item.opcion}`}
                className="flex items-center gap-4 border border-outline-variant/40 rounded p-4"
              >
                <img src={item.imagen} alt={item.nombre} className="w-20 h-20 object-cover rounded" />
                <div className="flex-1">
                  <p className="font-title-md text-title-md text-on-surface">{item.nombre}</p>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">
                    {item.opcion} · Cant. {item.cantidad}
                  </p>
                </div>
                <p className="font-title-md text-title-md text-on-surface">${fmt.format(item.precio * item.cantidad)}</p>
                <button
                  onClick={() => quitarItem(item.id, item.opcion)}
                  className="text-on-surface-variant hover:text-error p-1.5"
                  aria-label="Quitar"
                >
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>
            ))}
          </div>

          <div className="lg:col-span-4">
            <div className="bg-surface-container-lowest border border-outline-variant/40 rounded p-6 sticky top-28">
              <h2 className="font-headline-sm text-headline-sm text-on-surface mb-4">Resumen del Pedido</h2>
              <div className="flex justify-between font-body-md text-body-md text-on-surface-variant mb-2">
                <span>Subtotal</span>
                <span>${fmt.format(total)}</span>
              </div>
              <div className="flex justify-between font-body-md text-body-md text-on-surface-variant mb-4">
                <span>Envío</span>
                <span>{total >= 60000 ? "Gratis" : "$4.500"}</span>
              </div>
              <div className="flex justify-between font-title-md text-title-md text-on-surface border-t border-outline-variant/40 pt-4 mb-6">
                <span>Total</span>
                <span>${fmt.format(total >= 60000 ? total : total + 4500)}</span>
              </div>
              <PrimaryButton disabled={!estaAutenticado}>
                {estaAutenticado ? "Confirmar y Pagar" : "Iniciá sesión para pagar"}
              </PrimaryButton>
              {!estaAutenticado && (
                <p className="font-body-sm text-body-sm text-on-surface-variant text-center mt-3">
                  <Link to="/login" className="text-primary hover:underline">
                    Iniciá sesión
                  </Link>{" "}
                  o{" "}
                  <Link to="/registro" className="text-primary hover:underline">
                    creá tu cuenta
                  </Link>{" "}
                  para completar la compra.
                </p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
