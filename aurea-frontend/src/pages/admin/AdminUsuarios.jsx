import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { adminService } from "../../services/adminService";
import {
  Boton, Busqueda, Cargando, Chips, Encabezado, Estado, Indicador, Mensaje, Tarjeta, Vacio,
} from "../../components/admin/AdminUI";

/** Usuarios registrados y asignación del rol ADMIN (PATCH /api/usuarios/{id}/rol, solo ADMIN). */
export default function AdminUsuarios() {
  const { usuario: actual } = useAuth();
  const [usuarios, setUsuarios] = useState(null);
  const [rol, setRol] = useState("TODOS");
  const [busqueda, setBusqueda] = useState("");
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [ocupado, setOcupado] = useState(null);

  const cargar = useCallback(async () => {
    try {
      setUsuarios(await adminService.usuarios());
    } catch (e) {
      setError(e.message);
      setUsuarios((u) => u ?? []);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const cambiarRol = async (u) => {
    const nuevo = u.rol === "ADMIN" ? "CLIENTE" : "ADMIN";
    const texto = nuevo === "ADMIN"
      ? `¿Dar acceso de administración a ${u.nombre} ${u.apellido}? Podrá modificar el catálogo, ver pedidos y gestionar reposiciones.`
      : `¿Quitar el acceso de administración a ${u.nombre} ${u.apellido}?`;
    if (!window.confirm(texto)) return;
    setOcupado(u.id);
    setError("");
    setExito("");
    try {
      const actualizado = await adminService.cambiarRol(u.id, nuevo);
      setUsuarios((lista) => lista.map((x) => (x.id === u.id ? actualizado : x)));
      setExito(`${actualizado.nombre} ${actualizado.apellido} ahora tiene rol ${actualizado.rol}.`);
    } catch (e) {
      setError(e.message);
    } finally {
      setOcupado(null);
    }
  };

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return (usuarios ?? [])
      .filter((u) => rol === "TODOS" || u.rol === rol)
      .filter((u) => !texto || `${u.nombre} ${u.apellido} ${u.email}`.toLowerCase().includes(texto))
      .sort((a, b) => `${a.apellido} ${a.nombre}`.localeCompare(`${b.apellido} ${b.nombre}`, "es"));
  }, [usuarios, rol, busqueda]);

  if (!usuarios) return <Cargando texto="Cargando usuarios…" />;
  const admins = usuarios.filter((u) => u.rol === "ADMIN").length;

  return (
    <>
      <Encabezado
        etiqueta="Identidad y acceso"
        titulo="Usuarios y roles"
        descripcion="Solo quienes tienen rol ADMIN acceden a este panel. El backend vuelve a validar el rol en cada operación."
      />
      {exito && <Mensaje tono="exito" onCerrar={() => setExito("")}>{exito}</Mensaje>}
      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}

      <div className="mb-8 grid gap-5 md:grid-cols-3">
        <Indicador etiqueta="Usuarios registrados" icono="group" valor={usuarios.length} />
        <Indicador etiqueta="Administradores" icono="admin_panel_settings" tono="calido" valor={admins} />
        <Indicador etiqueta="Clientes" icono="person" valor={usuarios.length - admins} />
      </div>

      <Tarjeta className="mb-6 flex flex-col gap-4 p-4 lg:flex-row lg:items-center lg:justify-between">
        <Busqueda valor={busqueda} onCambiar={setBusqueda} placeholder="Buscá por nombre o email" />
        <Chips etiqueta="Filtrar por rol" valor={rol} onCambiar={setRol} opciones={[
          { valor: "TODOS", texto: "Todos", cantidad: usuarios.length },
          { valor: "ADMIN", texto: "Administradores", cantidad: admins },
          { valor: "CLIENTE", texto: "Clientes", cantidad: usuarios.length - admins },
        ]} />
      </Tarjeta>

      <Tarjeta className="overflow-hidden">
        {visibles.length === 0 ? (
          <Vacio icono="person_search" titulo="No hay usuarios para mostrar" />
        ) : (
          <ul className="divide-y divide-outline-variant/30">
            {visibles.map((u) => {
              const esYo = u.id === actual?.id;
              return (
                <li key={u.id} className="flex flex-col gap-4 px-6 py-4 sm:flex-row sm:items-center">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-fixed font-label-md text-label-md text-on-primary-fixed">
                    {`${u.nombre?.[0] ?? ""}${u.apellido?.[0] ?? ""}`.toUpperCase()}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="font-title-md text-title-md">
                      {u.nombre} {u.apellido} {esYo && <span className="font-body-sm text-body-sm text-on-surface-variant">(vos)</span>}
                    </p>
                    <p className="truncate font-body-sm text-body-sm text-on-surface-variant">{u.email}</p>
                  </div>
                  <Estado tono={u.rol === "ADMIN" ? "info" : "neutro"}>{u.rol}</Estado>
                  <Boton
                    variante={u.rol === "ADMIN" ? "peligro" : "contorno"}
                    icono={u.rol === "ADMIN" ? "remove_moderator" : "add_moderator"}
                    disabled={esYo || ocupado === u.id}
                    title={esYo ? "No podés cambiar tu propio rol" : undefined}
                    onClick={() => cambiarRol(u)}
                  >
                    {u.rol === "ADMIN" ? "Quitar admin" : "Hacer admin"}
                  </Boton>
                </li>
              );
            })}
          </ul>
        )}
      </Tarjeta>
    </>
  );
}
