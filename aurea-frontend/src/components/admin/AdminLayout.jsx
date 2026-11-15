import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export const SECCIONES_ADMIN = [
  { to: "/admin", end: true, icono: "space_dashboard", texto: "Panel" },
  { to: "/admin/productos", icono: "diamond", texto: "Productos" },
  { to: "/admin/pedidos", icono: "receipt_long", texto: "Pedidos" },
  { to: "/admin/usuarios", icono: "group", texto: "Usuarios" },
  { to: "/admin/reposiciones", icono: "inventory_2", texto: "Reposiciones" },
  { to: "/admin/avisos", icono: "notifications", texto: "Avisos" },
];

const claseEnlace = ({ isActive }) =>
  `flex items-center gap-3 rounded-lg px-4 py-2.5 font-body-md text-body-md transition-colors ${
    isActive ? "bg-primary-container text-on-primary shadow-sm" : "text-on-surface hover:bg-surface-container-high"
  }`;

const claseChip = ({ isActive }) =>
  `flex shrink-0 items-center gap-2 rounded-full px-4 py-2 font-label-md text-label-md ${
    isActive ? "bg-primary-container text-on-primary" : "bg-surface-container text-on-surface"
  }`;

/**
 * Estructura del panel de administración. Se monta detrás de ProtectedRoute con rol ADMIN;
 * el backend vuelve a exigir el rol en cada endpoint administrativo.
 */
export default function AdminLayout() {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const actual =
    [...SECCIONES_ADMIN].reverse().find((s) => (s.end ? pathname === s.to : pathname.startsWith(s.to))) ??
    SECCIONES_ADMIN[0];

  const salir = async () => {
    if (await cerrarSesion()) navigate("/", { replace: true });
  };

  return (
    <div className="min-h-screen bg-surface font-body-md text-body-md text-on-surface lg:flex">
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-outline-variant/30 bg-surface-container-low px-5 py-8 lg:flex">
        <Link to="/admin" className="mb-10 block px-2">
          <span className="block font-headline-sm text-[26px] leading-8 text-primary">Áurea Joyas</span>
          <span className="font-label-sm text-label-sm uppercase tracking-[0.14em] text-on-surface-variant">
            Panel de administración
          </span>
        </Link>
        <nav aria-label="Secciones de administración" className="flex-1 space-y-1">
          {SECCIONES_ADMIN.map((s) => (
            <NavLink key={s.to} to={s.to} end={s.end} className={claseEnlace}>
              <span className="material-symbols-outlined">{s.icono}</span>
              {s.texto}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-2 rounded-xl bg-surface-container-lowest p-4 font-label-sm text-label-sm uppercase tracking-wide text-on-surface shadow-sm">
          <span className="h-2 w-2 rounded-full bg-primary" aria-hidden="true" />
          Acceso exclusivo · rol ADMIN
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 border-b border-outline-variant/30 bg-surface/95 backdrop-blur">
          <div className="flex h-16 items-center justify-between gap-4 px-5 lg:px-10">
            <nav aria-label="Ubicación" className="flex min-w-0 items-center gap-2 font-label-md text-label-md">
              <Link to="/admin" className="text-on-surface hover:text-primary">
                Administración
              </Link>
              <span className="text-outline">/</span>
              <span className="truncate text-primary">{actual.texto}</span>
            </nav>
            <div className="flex items-center gap-2 sm:gap-5">
              <Link to="/" className="flex items-center gap-1.5 font-label-md text-label-md text-primary hover:underline">
                <span className="material-symbols-outlined">open_in_new</span>
                <span className="hidden sm:inline">Ver tienda</span>
              </Link>
              <div className="hidden items-center gap-3 border-l border-outline-variant/40 pl-5 sm:flex">
                <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-on-primary">
                  <span className="material-symbols-outlined">person</span>
                </span>
                <div className="leading-tight">
                  <p className="font-label-md text-label-md text-on-surface">
                    {usuario?.nombre} {usuario?.apellido}
                  </p>
                  <p className="font-label-sm text-label-sm uppercase text-primary">Rol: {usuario?.rol}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={salir}
                aria-label="Cerrar sesión"
                className="rounded p-2 text-on-surface-variant hover:bg-surface-container hover:text-primary"
              >
                <span className="material-symbols-outlined">logout</span>
              </button>
            </div>
          </div>
          <nav aria-label="Secciones de administración" className="thin-scroll flex gap-2 overflow-x-auto px-5 pb-3 lg:hidden">
            {SECCIONES_ADMIN.map((s) => (
              <NavLink key={s.to} to={s.to} end={s.end} className={claseChip}>
                <span className="material-symbols-outlined text-[18px]">{s.icono}</span>
                {s.texto}
              </NavLink>
            ))}
          </nav>
        </header>
        <main className="mx-auto max-w-[1400px] px-5 py-8 lg:px-10 lg:py-10">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
