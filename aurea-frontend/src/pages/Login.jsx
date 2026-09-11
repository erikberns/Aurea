import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { FormField, TextInput, PrimaryButton, Banner } from "../components/ui";

export default function Login() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", contrasena: "" });
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setCargando(true);
    try {
      // Llamada a iUsuarios.autenticar(credenciales)
      await iniciarSesion({ email: form.email, contrasena: form.contrasena });
      const destino = location.state?.from?.pathname || "/cuenta";
      navigate(destino, { replace: true });
    } catch (err) {
      setError(err.message || "No pudimos iniciar sesión. Intentá de nuevo.");
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="w-full px-6 lg:px-16 mx-auto py-16 flex justify-center">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <span className="font-label-sm text-label-sm uppercase tracking-[0.2em] text-primary font-semibold block mb-2">
            Servicio De Usuarios
          </span>
          <h1 className="font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface">
            Bienvenida de vuelta
          </h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Iniciá sesión para ver tus pedidos, tu lista de deseos y tus datos guardados.
          </p>
        </div>

        <form onSubmit={onSubmit} className="space-y-5 bg-surface-container-lowest border border-outline-variant/40 rounded p-6 sm:p-8 shadow-sm">
          {error && <Banner tone="error">{error}</Banner>}
          <FormField label="Email">
            <TextInput
              type="email"
              name="email"
              autoComplete="email"
              required
              value={form.email}
              onChange={onChange}
              placeholder="tu@email.com"
            />
          </FormField>
          <FormField label="Contraseña">
            <TextInput
              type="password"
              name="contrasena"
              autoComplete="current-password"
              required
              value={form.contrasena}
              onChange={onChange}
              placeholder="••••••••"
            />
          </FormField>
          <PrimaryButton type="submit" disabled={cargando}>
            {cargando ? "Ingresando…" : "Iniciar Sesión"}
          </PrimaryButton>
        </form>

        <p className="text-center font-body-md text-body-md text-on-surface-variant mt-6">
          ¿Todavía no tenés cuenta?{" "}
          <Link to="/registro" className="text-primary font-semibold hover:underline">
            Registrate acá
          </Link>
        </p>
      </div>
    </div>
  );
}
