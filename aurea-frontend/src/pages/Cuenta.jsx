import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { usuariosService } from "../services/usuariosService";
import { FormField, TextInput, PrimaryButton, SecondaryButton, Banner } from "../components/ui";


function TabPerfil({ usuario, onPerfilActualizado }) {
  const [form, setForm] = useState({ nombre: usuario.nombre, apellido: usuario.apellido || "", email: usuario.email });
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMensaje("");
    setGuardando(true);
    try {
      // Llamada a iUsuarios.actualizarPerfil(usuarioId, datosPerfil)
      const actualizado = await usuariosService.actualizarPerfil(usuario.id, form);
      onPerfilActualizado(actualizado);
      setMensaje("Perfil actualizado correctamente.");
    } catch (err) {
      setError(err.message || "No pudimos actualizar tu perfil.");
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form onSubmit={onSubmit} className="space-y-5 max-w-lg">
      {error && <Banner tone="error">{error}</Banner>}
      {mensaje && <Banner tone="success">{mensaje}</Banner>}
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nombre">
          <TextInput value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required />
        </FormField>
        <FormField label="Apellido">
          <TextInput value={form.apellido} onChange={(e) => setForm({ ...form, apellido: e.target.value })} />
        </FormField>
      </div>
      <FormField label="Email">
        <TextInput type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
      </FormField>
      <FormField label="Rol asignado">
        <TextInput value={usuario.rol} disabled className="opacity-70 cursor-not-allowed" />
      </FormField>
      <PrimaryButton type="submit" disabled={guardando} className="w-auto px-8">
        {guardando ? "Guardando…" : "Guardar Cambios"}
      </PrimaryButton>
    </form>
  );
}

function TabSeguridad({ usuario }) {
  const [form, setForm] = useState({ actual: "", nueva: "", repetir: "" });
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMensaje("");
    if (form.nueva !== form.repetir) {
      setError("La confirmación no coincide con la nueva contraseña.");
      return;
    }
    if (form.nueva.length < 8) {
      setError("La nueva contraseña debe tener al menos 8 caracteres.");
      return;
    }
    setGuardando(true);
    try {
      // Llamada a iUsuarios.cambiarContrasena(usuarioId, contrasenaActual, contrasenaNueva)
      await usuariosService.cambiarContrasena(usuario.id, form.actual, form.nueva);
      setMensaje("Tu contraseña se actualizó correctamente.");
      setForm({ actual: "", nueva: "", repetir: "" });
    } catch (err) {
      setError(err.message || "No pudimos cambiar tu contraseña.");
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form onSubmit={onSubmit} className="space-y-5 max-w-lg">
      {error && <Banner tone="error">{error}</Banner>}
      {mensaje && <Banner tone="success">{mensaje}</Banner>}
      <FormField label="Contraseña actual">
        <TextInput
          type="password"
          value={form.actual}
          onChange={(e) => setForm({ ...form, actual: e.target.value })}
          required
        />
      </FormField>
      <FormField label="Nueva contraseña">
        <TextInput
          type="password"
          value={form.nueva}
          onChange={(e) => setForm({ ...form, nueva: e.target.value })}
          required
        />
      </FormField>
      <FormField label="Repetir nueva contraseña">
        <TextInput
          type="password"
          value={form.repetir}
          onChange={(e) => setForm({ ...form, repetir: e.target.value })}
          required
        />
      </FormField>
      <PrimaryButton type="submit" disabled={guardando} className="w-auto px-8">
        {guardando ? "Actualizando…" : "Actualizar Contraseña"}
      </PrimaryButton>
    </form>
  );
}

function TabPedidos() {
  const fmt = new Intl.NumberFormat("es-AR");
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    import("../services/ordenesService").then(({ getMisPedidos }) => {
      getMisPedidos()
        .then(setPedidos)
        .catch(console.error)
        .finally(() => setLoading(false));
    });
  }, []);

  if (loading) return <p className="text-on-surface-variant">Cargando tus pedidos...</p>;
  if (pedidos.length === 0) return <p className="text-on-surface-variant">Aún no tenés pedidos.</p>;

  return (
    <div className="space-y-4 max-w-2xl">
      {pedidos.map((p) => (
        <div key={p.id} className="border border-outline-variant/40 rounded p-4 flex items-center justify-between">
          <div>
            <p className="font-title-md text-title-md text-on-surface">Orden #{p.id}</p>
            <p className="font-body-sm text-body-sm text-on-surface-variant">
              {new Date(p.orderDate).toLocaleDateString("es-AR")} · {p.items?.length || 0} producto{p.items?.length !== 1 ? "s" : ""}
            </p>
          </div>
          <div className="text-right">
            <span className="inline-block bg-secondary-container/60 text-on-secondary-container font-label-sm text-label-sm uppercase px-2 py-1 rounded-full mb-1">
              {p.status}
            </span>
            <p className="font-title-md text-title-md text-on-surface">${fmt.format(p.total)}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

export default function Cuenta() {
  const { usuario, cerrarSesion, actualizarUsuarioLocal } = useAuth();
  const [tab, setTab] = useState("perfil");
  const [perfil, setPerfil] = useState(usuario);
  const navigate = useNavigate();

  useEffect(() => {
    // Llamada a iUsuarios.consultarPerfil(usuarioId) al entrar a la sección
    usuariosService.consultarPerfil(usuario.id).then(setPerfil).catch(() => setPerfil(usuario));
  }, [usuario]);

  const onPerfilActualizado = (datos) => {
    setPerfil((prev) => ({ ...prev, ...datos }));
    actualizarUsuarioLocal(datos);
  };

  const onCerrarSesion = async () => {
    if (await cerrarSesion()) navigate("/");
  };

  const tabs = [
    { id: "perfil", label: "Mi Perfil", icon: "person" },
    { id: "seguridad", label: "Seguridad & Contraseña", icon: "lock" },
    { id: "pedidos", label: "Mis Pedidos", icon: "shopping_bag" },
  ];

  return (
    <div className="w-full px-6 lg:px-16 mx-auto py-10">
      <div className="bg-surface-container-lowest border border-outline-variant/40 rounded p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-secondary-container flex items-center justify-center font-headline-sm text-headline-sm text-on-secondary-container">
            {perfil?.nombre?.[0]?.toUpperCase()}
          </div>
          <div>
            <h1 className="font-headline-sm text-headline-sm text-on-surface">Hola, {perfil?.nombre}</h1>
            <p className="font-body-sm text-body-sm text-on-surface-variant">
              Gestioná tus joyas en camino, tus datos y tu seguridad.
            </p>
          </div>
        </div>
        <SecondaryButton onClick={onCerrarSesion} className="w-auto px-6">
          Cerrar Sesión
        </SecondaryButton>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        <aside className="lg:col-span-3">
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant mb-3">
            Portal de Cliente
          </p>
          <nav className="space-y-1">
            {tabs.map((t) => (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded font-body-md text-body-md text-left transition-colors ${
                  tab === t.id
                    ? "bg-secondary-container/50 text-on-secondary-container font-semibold"
                    : "text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                <span className="material-symbols-outlined text-lg">{t.icon}</span>
                {t.label}
              </button>
            ))}
          </nav>
        </aside>

        <div className="lg:col-span-9">
          {perfil && tab === "perfil" && <TabPerfil usuario={perfil} onPerfilActualizado={onPerfilActualizado} />}
          {perfil && tab === "seguridad" && <TabSeguridad usuario={perfil} />}
          {tab === "pedidos" && <TabPedidos />}
        </div>
      </div>
    </div>
  );
}
