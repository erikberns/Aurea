
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import Logo from "./Logo";
import { esAdmin } from "../utils/admin";
import { THEMES, useTheme } from "../context/ThemeContext";

const NAV_LINKS = [
  { label: "Novedades", to: "/" },
  { label: "Anillos", to: "/catalogo?categoria=Anillos apilables" },
  { label: "Collares", to: "/catalogo?categoria=Collares %26 Medallas" },
  { label: "Pulseras", to: "/catalogo?categoria=Pulseras %26 Eslabones" },
  { label: "Pendientes", to: "/catalogo?categoria=Pendientes %26 Huggies" },
];

export default function Header() {
  const { usuario, estaAutenticado } = useAuth();
  const { cantidadTotal } = useCart();
  const location = useLocation();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();

  const [searchTerm, setSearchTerm] = useState("");
  const [menuAbierto, setMenuAbierto] = useState(false);

  const handleSearch = (e) => {
    if (e.key === "Enter" && searchTerm.trim() !== "") {
      navigate(`/catalogo?search=${encodeURIComponent(searchTerm.trim())}`);
      setSearchTerm("");
      setMenuAbierto(false);
    }
  };

  const currentPath = location.pathname + decodeURIComponent(location.search).replace(/&/g, "%26");

  return (
    <>
      <div className="bg-surface-container text-on-surface-variant py-2.5 px-4 text-center border-b border-outline-variant/30">
        <div className="max-w-7xl mx-auto flex items-center justify-center gap-2">
          <span className="material-symbols-outlined text-primary text-base">
            local_shipping
          </span>
          <p className="font-label-sm text-label-sm tracking-wide">
            ENVÍO GRATUITO EN COMPRAS SUPERIORES A $60.000 · HASTA 3 CUOTAS SIN INTERÉS CON MERCADO PAGO
          </p>
        </div>
      </div>

      <header className="bg-surface/90 backdrop-blur-md sticky top-0 z-50 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] border-b border-outline-variant/30">

        <div className="w-full px-4 md:px-12 max-w-7xl mx-auto flex items-center justify-between h-20">

          <Link
            to="/"
            onClick={() => setMenuAbierto(false)}
            className="flex items-center gap-2 group shrink-0 active:scale-[0.99] transition-transform duration-150 py-1 mr-4 lg:mr-14"
            aria-label="Áurea Joyas - Inicio"
          >
            <Logo variant={theme === "ciruela" ? "light" : "dark"} className="h-8 md:h-9 w-auto" />
          </Link>

          <nav className="hidden md:flex items-center gap-10">
            {NAV_LINKS.map((link) => {
              const isActive = link.to === "/"
                ? currentPath === "/"
                : currentPath === link.to;

              return (
                <Link
                  key={link.label}
                  to={link.to}
                  className={`pb-1 font-label-md text-label-md transition-colors duration-200 ${
                    isActive
                      ? "border-b-2 border-primary text-primary font-semibold"
                      : "text-on-surface-variant hover:text-on-surface"
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
          </nav>

          <div className="hidden lg:flex items-center relative mx-6">
            <input
              className="h-10 pl-9 pr-4 py-2 w-52 bg-surface-container-lowest border border-outline-variant/50 rounded-lg text-body-sm font-body-sm focus:w-64 focus:border-primary-container focus:ring-1 focus:ring-primary-container transition-all outline-none text-on-surface"
              placeholder="Buscar dijes, aros, tallas..."
              type="search"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={handleSearch}
            />
            <span className="material-symbols-outlined text-outline absolute left-2.5 text-[18px] pointer-events-none">
              search
            </span>
          </div>

          <div className="flex items-center gap-3 sm:gap-5">

            <label className="hidden sm:flex items-center gap-2 text-on-surface-variant" title="Elegir tema">
              <span className="material-symbols-outlined text-[18px]" aria-hidden="true">palette</span>
              <select aria-label="Tema de color" value={theme} onChange={(event) => setTheme(event.target.value)} className="max-w-32 bg-surface border border-outline-variant/50 rounded px-2 py-1 text-xs text-on-surface focus:border-primary outline-none">
                {THEMES.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
              </select>
            </label>

            <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded bg-surface-container text-on-surface-variant font-label-sm text-label-sm border border-outline-variant/30">
              <span className="font-bold">ARS ($)</span>
            </div>

            {esAdmin(usuario) && (
              <Link
                to="/admin"
                className="flex items-center gap-1 rounded border border-primary-container/40 px-2.5 py-1 font-label-md text-label-md text-primary hover:bg-primary-container/10"
                aria-label="Panel de administración"
              >
                <span className="material-symbols-outlined text-[18px]">
                  admin_panel_settings
                </span>
                <span className="hidden md:inline">Panel admin</span>
              </Link>
            )}

            <button
              type="button"
              aria-label="Favoritos"
              className="p-2 text-on-surface-variant hover:text-primary transition-colors duration-200 relative active:scale-[0.99]"
            >
              <span className="material-symbols-outlined text-[22px]">
                favorite
              </span>
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
              <span className="material-symbols-outlined text-[24px]">
                shopping_bag
              </span>
              <span className="absolute top-1 right-1 bg-primary-container text-on-primary rounded-full w-4 h-4 text-[10px] font-bold flex items-center justify-center">
                {cantidadTotal}
              </span>
            </Link>

            <button
              type="button"
              onClick={() => setMenuAbierto(!menuAbierto)}
              aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
              aria-expanded={menuAbierto}
              aria-controls="menu-mobile"
              className="md:hidden p-2 text-on-surface-variant hover:text-primary transition-colors"
            >
              <span className="material-symbols-outlined text-[26px]">
                {menuAbierto ? "close" : "menu"}
              </span>
            </button>

          </div>
        </div>

        {menuAbierto && (
          <div
            id="menu-mobile"
            className="md:hidden border-t border-outline-variant/30 bg-surface px-6 py-5 shadow-md"
          >
            <nav className="flex flex-col gap-4" aria-label="Navegación móvil">

              {NAV_LINKS.map((link) => {
                const isActive = link.to === "/"
                  ? location.pathname === "/" && !location.search
                  : currentPath === link.to;

                return (
                  <Link
                    key={link.label}
                    to={link.to}
                    onClick={() => setMenuAbierto(false)}
                    className={`font-label-md text-label-md py-1 transition-colors ${
                      isActive
                        ? "text-primary font-semibold"
                        : "text-on-surface-variant hover:text-primary"
                    }`}
                  >
                    {link.label}
                  </Link>
                );
              })}

              {esAdmin(usuario) && (
                <Link
                  to="/admin"
                  onClick={() => setMenuAbierto(false)}
                  className="font-label-md text-label-md text-primary py-1"
                >
                  Panel de administración
                </Link>
              )}

            </nav>

            <div className="mt-5 pt-4 border-t border-outline-variant/30">
              <label htmlFor="mobile-theme" className="block font-label-sm text-label-sm text-on-surface-variant mb-2">
                Tema de color
              </label>
              <select id="mobile-theme" value={theme} onChange={(event) => setTheme(event.target.value)} className="w-full h-11 mb-5 bg-surface-container-lowest border border-outline-variant/50 rounded px-3 text-body-sm text-on-surface focus:border-primary outline-none">
                {THEMES.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
              </select>
              <label
                htmlFor="mobile-search"
                className="block font-label-sm text-label-sm text-on-surface-variant mb-2"
              >
                Buscar joyas
              </label>

              <div className="relative">
                <input
                  id="mobile-search"
                  type="search"
                  placeholder="Buscar dijes, aros, tallas..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={handleSearch}
                  className="w-full h-11 pl-10 pr-4 bg-surface-container-lowest border border-outline-variant/50 rounded-lg text-body-sm text-on-surface focus:border-primary outline-none"
                />

                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[20px] pointer-events-none">
                  search
                </span>
              </div>
            </div>

          </div>
        )}

      </header>
    </>
  );
}
