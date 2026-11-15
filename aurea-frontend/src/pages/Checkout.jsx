import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { PrimaryButton, SecondaryButton } from "../components/ui";
import { httpRequest } from "../api/httpClient";

const fmt = new Intl.NumberFormat("es-AR");

export default function Checkout() {
  const { items, quitarItem, total, envio, totalCompra, refrescar, error: carritoError, cargando } = useCart();
  const { estaAutenticado } = useAuth();
  const [shippingAddress, setShippingAddress] = useState("");
  const [addressError, setAddressError] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [configPago, setConfigPago] = useState(null);
  const [metodoPago, setMetodoPago] = useState("");
  const [errorPago, setErrorPago] = useState("");

  // La pasarela activa (Stripe en modo de prueba o simulada) define los métodos de prueba disponibles.
  // El navegador solo envía un código de la lista: nunca números de tarjeta.
  useEffect(() => {
    if (!estaAutenticado) return;
    let activo = true;
    httpRequest("/pagos/configuracion")
      .then((config) => {
        if (!activo) return;
        setConfigPago(config);
        setMetodoPago((actual) => actual || config.metodos[0]?.codigo || "");
      })
      .catch(() => activo && setConfigPago(null));
    return () => { activo = false; };
  }, [estaAutenticado]);

  const navigate = useNavigate();

  const processPayment = async () => {
    if (!shippingAddress.trim()) { setAddressError(true); return; }
    if (!metodoPago) { setErrorPago("Elegí un método de pago."); return; }
    setAddressError(false); setErrorPago(""); setLoading(true);
    try {
      const orden = await httpRequest("/carrito/checkout", {
        method: "POST",
        body: { direccion: shippingAddress, metodoPago },
      });
      await refrescar();
      alert(`Pago aprobado. Pedido #${orden.id} confirmado por $${fmt.format(orden.total)}.`);
      navigate("/cuenta");
    } catch (error) {
      // 402: tarjeta rechazada. El pedido no se registró y la bolsa se conserva.
      setErrorPago(error.message);
      await refrescar();
    }
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
                  setErrorPago("");
                  setShowPaymentModal(true);
                }}
              >
                {loading ? "Procesando..." : (estaAutenticado ? "Confirmar pedido" : "Iniciá sesión para comprar")}
              </PrimaryButton>
              {errorPago && (
                <p role="alert" className="font-body-sm text-body-sm text-error mt-3">{errorPago}</p>
              )}
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

      <p className="mt-6 text-sm text-on-surface-variant">
        Pagos en modo de prueba{configPago ? ` (${configPago.proveedor === "stripe" ? "Stripe test" : "pasarela simulada"})` : ""}: no se realizan cobros reales.
      </p>
      {carritoError && <p role="alert" className="mt-4 text-error">{carritoError}</p>}

      {showPaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-surface-container-high/60 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-[0_16px_40px_-8px_rgba(30,27,24,0.12)] w-full max-w-md p-6 border border-outline-variant/30">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-2">
              Método de pago
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-6">
              Entorno de prueba{configPago?.proveedor === "stripe" ? " de Stripe" : ""}. Elegí una tarjeta de prueba;
              no ingreses datos reales.
            </p>
            {!configPago ? (
              <p role="alert" className="font-body-sm text-body-sm text-error mb-8">
                No se pudo obtener la configuración de pagos. Intentá nuevamente.
              </p>
            ) : (
              <fieldset className="space-y-3 mb-8">
                <legend className="sr-only">Tarjetas de prueba</legend>
                {configPago.metodos.map((metodo) => (
                  <label
                    key={metodo.codigo}
                    className={`flex items-center gap-3 border rounded p-3 cursor-pointer ${
                      metodoPago === metodo.codigo ? "border-primary" : "border-outline-variant/40"
                    }`}
                  >
                    <input
                      type="radio"
                      name="metodoPago"
                      value={metodo.codigo}
                      checked={metodoPago === metodo.codigo}
                      onChange={() => setMetodoPago(metodo.codigo)}
                    />
                    <span className="flex-1 font-body-md text-body-md text-on-surface">{metodo.descripcion}</span>
                    <span className={`font-label-sm text-label-sm ${metodo.aprobacionEsperada ? "text-primary" : "text-error"}`}>
                      {metodo.aprobacionEsperada ? "Aprueba" : "Rechaza"}
                    </span>
                  </label>
                ))}
              </fieldset>
            )}
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
                disabled={loading || !configPago || !metodoPago}
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
