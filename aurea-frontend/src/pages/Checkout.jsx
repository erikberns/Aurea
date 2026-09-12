import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { PrimaryButton } from "../components/ui";
import { createOrder, confirmOrder } from "../services/ordenesService";

const fmt = new Intl.NumberFormat("es-AR");

export default function Checkout() {
  const { items, quitarItem, total, vaciarCarrito } = useCart();
  const { estaAutenticado } = useAuth();
  const [shippingAddress, setShippingAddress] = useState("");
  const [addressError, setAddressError] = useState(false);
  const [loading, setLoading] = useState(false);

  // Billing states
  const [showBilling, setShowBilling] = useState(false);
  const [cardName, setCardName] = useState("");
  const [cardNumber, setCardNumber] = useState("");
  const [expiry, setExpiry] = useState("");
  const [cvv, setCvv] = useState("");

  const navigate = useNavigate();

  const openBillingForm = () => {
    if (!shippingAddress.trim()) {
      setAddressError(true);
      return;
    }
    setAddressError(false);
    setShowBilling(true);
  };

  const processPayment = async (e) => {
    e.preventDefault();
    if (!cardName || !cardNumber || !expiry || !cvv) {
      alert("Por favor completá todos los datos de facturación.");
      return;
    }
    setLoading(true);
    try {
      const formattedItems = items.map(item => ({
        productId: item.id,
        quantity: item.cantidad
      }));
      const orden = await createOrder(shippingAddress, formattedItems);
      await confirmOrder(orden.id);
      vaciarCarrito();
      alert("¡Compra confirmada con éxito!");
      navigate("/");
    } catch (error) {
      console.error(error);
      alert("Hubo un error al procesar tu compra.");
    } finally {
      setLoading(false);
      setShowBilling(false);
    }
  };

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
              <div className="mb-4">
                <label className="block font-label-md text-label-md text-on-surface mb-2">
                  Dirección de Envío
                </label>
                <input
                  type="text"
                  value={shippingAddress}
                  onChange={(e) => {
                    setShippingAddress(e.target.value);
                    if (e.target.value.trim()) setAddressError(false);
                  }}
                  placeholder="Ej: Av. Libertador 1234, CABA"
                  className={`w-full border rounded p-2 font-body-sm text-body-sm text-on-surface bg-surface-container-lowest ${
                    addressError ? "border-error" : "border-outline-variant"
                  }`}
                  disabled={!estaAutenticado || loading}
                />
                {addressError && (
                  <p className="font-body-sm text-body-sm text-error mt-1">Por favor ingresá una dirección de envío.</p>
                )}
              </div>
              <div className="flex justify-between font-title-md text-title-md text-on-surface border-t border-outline-variant/40 pt-4 mb-6">
                <span>Total</span>
                <span>${fmt.format(total >= 60000 ? total : total + 4500)}</span>
              </div>
              <PrimaryButton 
                disabled={!estaAutenticado || loading} 
                onClick={openBillingForm}
              >
                {loading ? "Procesando..." : (estaAutenticado ? "Ingresar Datos de Pago" : "Iniciá sesión para pagar")}
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

      {/* Modal de Facturación */}
      {showBilling && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#1e1b18]/60 p-4">
          <div className="bg-surface-container-lowest border border-outline-variant/40 rounded-sm p-6 max-w-md w-full relative shadow-lg">
            <button
              onClick={() => setShowBilling(false)}
              className="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface transition-colors"
              disabled={loading}
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h2 className="font-headline-sm text-headline-sm text-on-surface mb-6">Datos de Facturación</h2>
            <form onSubmit={processPayment} className="space-y-4">
              <div>
                <label className="block font-label-md text-label-md text-on-surface mb-2">Nombre en la tarjeta</label>
                <input
                  type="text"
                  value={cardName}
                  onChange={(e) => setCardName(e.target.value)}
                  placeholder="Ej: Juan Pérez"
                  className="w-full border border-outline-variant rounded-sm p-2.5 font-body-sm text-body-sm text-on-surface bg-surface-container-lowest focus:border-primary focus:outline-none transition-colors"
                  disabled={loading}
                  required
                />
              </div>
              <div>
                <label className="block font-label-md text-label-md text-on-surface mb-2">Número de tarjeta</label>
                <input
                  type="text"
                  value={cardNumber}
                  onChange={(e) => setCardNumber(e.target.value)}
                  placeholder="Ej: 4500 1234 5678 9010"
                  className="w-full border border-outline-variant rounded-sm p-2.5 font-body-sm text-body-sm text-on-surface bg-surface-container-lowest focus:border-primary focus:outline-none transition-colors"
                  disabled={loading}
                  maxLength="19"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-label-md text-label-md text-on-surface mb-2">Vencimiento</label>
                  <input
                    type="text"
                    value={expiry}
                    onChange={(e) => setExpiry(e.target.value)}
                    placeholder="MM/AA"
                    className="w-full border border-outline-variant rounded-sm p-2.5 font-body-sm text-body-sm text-on-surface bg-surface-container-lowest focus:border-primary focus:outline-none transition-colors"
                    disabled={loading}
                    maxLength="5"
                    required
                  />
                </div>
                <div>
                  <label className="block font-label-md text-label-md text-on-surface mb-2">CVV</label>
                  <input
                    type="password"
                    value={cvv}
                    onChange={(e) => setCvv(e.target.value)}
                    placeholder="123"
                    className="w-full border border-outline-variant rounded-sm p-2.5 font-body-sm text-body-sm text-on-surface bg-surface-container-lowest focus:border-primary focus:outline-none transition-colors"
                    disabled={loading}
                    maxLength="4"
                    required
                  />
                </div>
              </div>
              <div className="pt-4">
                <PrimaryButton type="submit" disabled={loading} className="w-full py-3">
                  {loading ? "Procesando pago..." : `Pagar $${fmt.format(total >= 60000 ? total : total + 4500)}`}
                </PrimaryButton>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
