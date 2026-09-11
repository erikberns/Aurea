import { createContext, useContext, useEffect, useState, useMemo } from "react";

const CART_KEY = "aurea_carrito";
const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    const raw = localStorage.getItem(CART_KEY);
    return raw ? JSON.parse(raw) : [];
  });

  // Persistencia del estado del carrito ante cada cambio: en el frontend
  // esto simula el comportamiento stateful de ServicioDeCarrito, que
  // conserva la selección del cliente entre solicitudes.
  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(items));
  }, [items]);

  const agregarItem = (joya, opcion) => {
    setItems((prev) => {
      const existente = prev.find((i) => i.id === joya.id && i.opcion === opcion);
      if (existente) {
        return prev.map((i) =>
          i.id === joya.id && i.opcion === opcion ? { ...i, cantidad: i.cantidad + 1 } : i
        );
      }
      return [...prev, { id: joya.id, nombre: joya.nombre, precio: joya.precio, imagen: joya.imagen, opcion, cantidad: 1 }];
    });
  };

  const quitarItem = (id, opcion) => {
    setItems((prev) => prev.filter((i) => !(i.id === id && i.opcion === opcion)));
  };

  const vaciarCarrito = () => setItems([]);

  const total = useMemo(() => items.reduce((acc, i) => acc + i.precio * i.cantidad, 0), [items]);
  const cantidadTotal = useMemo(() => items.reduce((acc, i) => acc + i.cantidad, 0), [items]);

  return (
    <CartContext.Provider value={{ items, agregarItem, quitarItem, vaciarCarrito, total, cantidadTotal }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart debe usarse dentro de <CartProvider>");
  return ctx;
}
