import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

const NAV_LINKS = [
  { label: "Novedades", to: "/" },
  { label: "Anillos", to: "/catalogo?categoria=Anillos apilables" },
  { label: "Collares", to: "/catalogo?categoria=Collares %26 Medallas" },
  { label: "Pulseras", to: "/catalogo?categoria=Pulseras %26 Eslabones" },
  { label: "Pendientes", to: "/catalogo?categoria=Pendientes %26 Huggies" },
  { label: "Regalos", to: "/catalogo" },
];

export default function Header() {
  const { usuario, estaAutenticado } = useAuth();
  const { cantidadTotal } = useCart();

  return (
    <>
      <div className="bg-surface-container text-on-surface-variant py-2.5 px-4 text-center border-b border-outline-variant/30">
        <div className="max-w-7xl mx-auto flex items-center justify-center gap-2">
          <span className="material-symbols-outlined text-primary text-base">local_shipping</span>
          <p className="font-label-sm text-label-sm tracking-wide">
            ENVÍO GRATUITO EN COMPRAS SUPERIORES A $60.000 · HASTA 3 CUOTAS SIN INTERÉS CON MERCADO PAGO
          </p>
        </div>
      </div>

      <header className="bg-surface/90 backdrop-blur-md sticky top-0 z-50 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] border-b border-outline-variant/30">
        <div className="w-full px-6 md:px-12 max-w-7xl mx-auto flex items-center justify-between h-20">
          <Link
            to="/"
            className="font-headline-md text-headline-md tracking-tight text-on-surface flex items-center gap-2 active:scale-[0.99] transition-transform duration-150"
          >
            <span className="text-primary-container font-serif italic text-3xl font-normal">Á</span>
            <span className="font-semibold tracking-normal">Áurea Joyas</span>
          </Link>

          <nav className="hidden md:flex items-center gap-8">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.label}
                to={link.to}
                end={link.to === "/"}
                className={({ isActive }) =>
                  `pb-1 font-label-md text-label-md transition-colors duration-200 ${
                    isActive
                      ? "border-b-2 border-primary text-primary font-semibold"
                      : "text-on-surface-variant hover:text-on-surface"
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-4 lg:gap-6">
            <div className="hidden lg:flex items-center relative">
              <input
                className="h-10 pl-9 pr-4 py-2 w-52 bg-surface-container-lowest border border-outline-variant/50 rounded-lg text-body-sm font-body-sm focus:w-64 focus:border-primary-container focus:ring-1 focus:ring-primary-container transition-all outline-none text-on-surface"
                placeholder="Buscar dijes, aros, tallas..."
                type="search"
              />
              <span className="material-symbols-outlined text-outline absolute left-2.5 text-[18px] pointer-events-none">
                search
              </span>
            </div>

            <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded bg-surface-container text-on-surface-variant font-label-sm text-label-sm border border-outline-variant/30">
              <span className="font-bold">ARS ($)</span>
            </div>

            <div className="flex items-center gap-3">
              <button aria-label="Favoritos" className="p-2 text-on-surface-variant hover:text-primary transition-colors duration-200 relative active:scale-[0.99]">
                <span className="material-symbols-outlined text-[22px]">favorite</span>
              </button>

              <Link
                to={estaAutenticado ? "/cuenta" : "/login"}
                aria-label="Mi cuenta"
                className="flex items-center gap-2 p-1 text-on-surface-variant hover:text-primary transition-colors duration-200 active:scale-[0.99]"
              >
                <div className="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center text-on-primary-fixed font-semibold text-xs border border-primary-container/30">
                  {estaAutenticado ? usuario?.nombre?.slice(0, 2).toUpperCase() : "?"}
                </div>
                <span className="hidden xl:inline-block font-label-md text-label-md text-on-surface">
                  {estaAutenticado ? usuario?.nombre : "Ingresar"}
                </span>
              </Link>

              <Link
                to="/checkout"
                aria-label="Bolsa de compras"
                className="p-2 text-on-surface-variant hover:text-primary transition-colors duration-200 relative active:scale-[0.99]"
              >
                <span className="material-symbols-outlined text-[24px]">shopping_bag</span>
                <span className="absolute top-1 right-1 bg-primary-container text-on-primary rounded-full w-4 h-4 text-[10px] font-bold flex items-center justify-center">
                  {cantidadTotal}
                </span>
              </Link>
            </div>
          </div>
        </div>
      </header>
    </>
  );
}
