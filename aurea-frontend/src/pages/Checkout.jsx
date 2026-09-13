import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { PrimaryButton, SecondaryButton, FormField, TextInput } from "../components/ui";
import { httpRequest } from "../api/httpClient";

const fmt = new Intl.NumberFormat("es-AR");

export default function Checkout() {
  const { items, quitarItem, total, envio, totalCompra, refrescar, error: carritoError, cargando } = useCart();
  const { estaAutenticado } = useAuth();
  const [shippingAddress, setShippingAddress] = useState("");
  const [addressError, setAddressError] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [cardNumber, setCardNumber] = useState("");
  const [expiry, setExpiry] = useState("");

  const handleCardNumberChange = (e) => {
    let val = e.target.value.replace(/\D/g, "");
    val = val.substring(0, 16);
    const groups = val.match(/.{1,4}/g);
    setCardNumber(groups ? groups.join(" ") : val);
  };

  const handleExpiryChange = (e) => {
    let val = e.target.value.replace(/\D/g, "");
    val = val.substring(0, 4);
    if (val.length >= 3) {
      setExpiry(`${val.substring(0, 2)}/${val.substring(2)}`);
    } else {
      setExpiry(val);
    }
  };

  const navigate = useNavigate();

  const processPayment = async () => {
    if (!shippingAddress.trim()) { setAddressError(true); return; }
    setAddressError(false); setLoading(true);
    try {
      const orden = await httpRequest("/carrito/checkout", { method: "POST", body: { direccion: shippingAddress } });
      await refrescar();
      alert(`Pedido #${orden.id} confirmado por $${fmt.format(orden.total)}. No se realizó un cobro.`);
      navigate("/cuenta");
    } catch (error) { alert(error.message); await refrescar(); }
    finally { setLoading(false); }
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
                <img src={item.imagen || "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop"} alt={item.nombre} className="w-20 h-20 object-cover rounded" />
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
                <span>{envio === 0 ? "Gratis" : `$${fmt.format(envio)}`}</span>
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
                  disabled={!estaAutenticado || loading || cargando}
                />
                {addressError && (
                  <p className="font-body-sm text-body-sm text-error mt-1">Por favor ingresá una dirección de envío.</p>
                )}
              </div>
              <div className="flex justify-between font-title-md text-title-md text-on-surface border-t border-outline-variant/40 pt-4 mb-6">
                <span>Total</span>
                <span>${fmt.format(totalCompra)}</span>
              </div>
              <PrimaryButton
                disabled={!estaAutenticado || loading || cargando}
                onClick={() => {
                  if (!shippingAddress.trim()) { setAddressError(true); return; }
                  setAddressError(false);
                  setShowPaymentModal(true);
                }}
              >
                {loading ? "Procesando..." : (estaAutenticado ? "Confirmar pedido" : "Iniciá sesión para comprar")}
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

      <p className="mt-6 text-sm text-on-surface-variant">La confirmación registra el pedido. El pago externo todavía no está integrado.</p>
      {carritoError && <p role="alert" className="mt-4 text-error">{carritoError}</p>}

      {showPaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-surface-container-high/60 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-[0_16px_40px_-8px_rgba(30,27,24,0.12)] w-full max-w-md p-6 border border-outline-variant/30">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-6">
              Detalles de Pago
            </h3>
            <div className="space-y-4 mb-8">
              <FormField label="Número de Tarjeta">
                <TextInput 
                  placeholder="0000 0000 0000 0000" 
                  value={cardNumber}
                  onChange={handleCardNumberChange}
                />
              </FormField>
              <div className="grid grid-cols-2 gap-4">
                <FormField label="Vencimiento">
                  <TextInput 
                    placeholder="MM/AA" 
                    value={expiry}
                    onChange={handleExpiryChange}
                  />
                </FormField>
                <FormField label="CVV">
                  <TextInput placeholder="123" />
                </FormField>
              </div>
              <FormField label="Nombre en la tarjeta">
                <TextInput placeholder="Juan Pérez" />
              </FormField>
            </div>
            <div className="flex justify-end gap-3">
              <SecondaryButton
                onClick={() => setShowPaymentModal(false)}
                disabled={loading}
                className="w-auto px-6 h-11"
              >
                Cancelar
              </SecondaryButton>
              <PrimaryButton
                onClick={() => {
                  setShowPaymentModal(false);
                  processPayment();
                }}
                disabled={loading}
                className="w-auto px-6 h-11"
              >
                Pagar ${fmt.format(totalCompra)}
              </PrimaryButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
