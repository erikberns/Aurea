import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { FormField, TextInput, PrimaryButton, Banner } from "../components/ui";

export default function Registro() {
  const { registrarse, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    nombre: "",
    apellido: "",
    email: "",
    contrasena: "",
    confirmarContrasena: "",
  });
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (form.contrasena !== form.confirmarContrasena) {
      setError("Las contraseñas no coinciden.");
      return;
    }
    if (form.contrasena.length < 8) {
      setError("La contraseña debe tener al menos 8 caracteres.");
      return;
    }

    setCargando(true);
    try {
      // Llamada a iUsuarios.registrarCliente(datosRegistro)
      await registrarse({
        nombre: form.nombre,
        apellido: form.apellido,
        email: form.email,
        contrasena: form.contrasena,
      });
      // Tras registrar, autenticamos directamente para pasar a /cuenta
      await iniciarSesion({ email: form.email, contrasena: form.contrasena });
      navigate("/cuenta", { replace: true });
    } catch (err) {
      setError(err.message || "No pudimos crear tu cuenta. Intentá de nuevo.");
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
            Creá tu cuenta
          </h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Sumate a Áurea y accedé a envío gratis, lista de deseos y seguimiento de pedidos.
          </p>
        </div>

        <form onSubmit={onSubmit} className="space-y-5 bg-surface-container-lowest border border-outline-variant/40 rounded p-6 sm:p-8 shadow-sm">
          {error && <Banner tone="error">{error}</Banner>}
          <div className="grid grid-cols-2 gap-4">
            <FormField label="Nombre">
              <TextInput name="nombre" required value={form.nombre} onChange={onChange} placeholder="Clara" />
            </FormField>
            <FormField label="Apellido">
              <TextInput name="apellido" required value={form.apellido} onChange={onChange} placeholder="Gómez" />
            </FormField>
          </div>
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
              autoComplete="new-password"
              required
              value={form.contrasena}
              onChange={onChange}
              placeholder="Mínimo 8 caracteres"
            />
          </FormField>
          <FormField label="Confirmar contraseña">
            <TextInput
              type="password"
              name="confirmarContrasena"
              autoComplete="new-password"
              required
              value={form.confirmarContrasena}
              onChange={onChange}
              placeholder="Repetí tu contraseña"
            />
          </FormField>
          <PrimaryButton type="submit" disabled={cargando}>
            {cargando ? "Creando cuenta…" : "Crear Cuenta"}
          </PrimaryButton>
        </form>

        <p className="text-center font-body-md text-body-md text-on-surface-variant mt-6">
          ¿Ya tenés cuenta?{" "}
          <Link to="/login" className="text-primary font-semibold hover:underline">
            Iniciá sesión
          </Link>
        </p>
      </div>
    </div>
  );
}
