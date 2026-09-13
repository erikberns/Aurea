import { createContext, useContext, useEffect, useRef, useState, useCallback } from "react";
import { httpRequest } from "../api/httpClient";
import { useAuth } from "./AuthContext";
const CartContext = createContext(null);
const VACIO = { items: [], subtotal: 0, envio: 0, total: 0, cantidadTotal: 0 };
export function CartProvider({ children }) {
  const { usuario, cargando: sesionCargando } = useAuth();
  const [carrito, setCarrito] = useState(VACIO);
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  // Ordena solicitudes de esta pestaña y evita sobrescribir con respuestas antiguas.
  const cola = useRef(Promise.resolve());
  const ejecutar = useCallback((path, opciones) => {
    const tarea = cola.current.then(async () => {
      setCargando(true);
      try {
        const actualizado = await httpRequest(path, opciones);
        setCarrito(actualizado); setError(""); return actualizado;
      } catch (e) { setError(e.message); throw e; }
      finally { setCargando(false); }
    });
    cola.current = tarea.catch(() => {});
    return tarea;
  }, []);
  const refrescar = useCallback(() => ejecutar("/carrito").catch(() => setCarrito(VACIO)), [ejecutar]);
  useEffect(() => { if (!sesionCargando) refrescar(); }, [usuario?.id, sesionCargando, refrescar]);
  useEffect(() => {
    window.addEventListener("aurea:carrito-cambio", refrescar);
    window.addEventListener("focus", refrescar);
    return () => {
      window.removeEventListener("aurea:carrito-cambio", refrescar);
      window.removeEventListener("focus", refrescar);
    };
  }, [refrescar]);
  const agregarItem = async joya => {
    try { await ejecutar("/carrito/items", { method: "POST", body: { productoId: joya.id, cantidad: 1 } }); return true; }
    catch (e) { alert(e.message); return false; }
  };
  const quitarItem = id => ejecutar(`/carrito/items/${id}`, { method: "DELETE" }).catch(e => alert(e.message));
  const vaciarCarrito = () => ejecutar("/carrito", { method: "DELETE" });
  return <CartContext.Provider value={{ ...carrito, total: carrito.subtotal, totalCompra: carrito.total,
    agregarItem, quitarItem, vaciarCarrito, refrescar, error, cargando }}>{children}</CartContext.Provider>;
}
export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart debe usarse dentro de CartProvider");
  return ctx;
}
