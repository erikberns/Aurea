import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminService } from "../../services/adminService";
import { fechaHora } from "../../utils/admin";
import { Boton, Cargando, Criterio, Encabezado, Mensaje, Tarjeta, Vacio } from "../../components/admin/AdminUI";

/**
 * Avisos administrativos. Los genera el suscriptor de Notificaciones al consumir el evento
 * ReposicionRecibida del tópico de RabbitMQ (pub/sub), independiente del suscriptor de inventario.
 */
export default function AdminAvisos() {
  const [avisos, setAvisos] = useState(null);
  const [error, setError] = useState("");

  const cargar = useCallback(async () => {
    try {
      const lista = await adminService.avisos();
      setAvisos([...lista].sort((a, b) => new Date(b.creada) - new Date(a.creada)));
      setError("");
    } catch (e) {
      setError(e.message);
      setAvisos((a) => a ?? []);
    }
  }, []);

  useEffect(() => {
    cargar();
    const timer = setInterval(cargar, 10000);
    return () => clearInterval(timer);
  }, [cargar]);

  if (!avisos) return <Cargando texto="Cargando avisos…" />;

  return (
    <>
      <Encabezado
        etiqueta="Bitácora operativa"
        titulo="Avisos administrativos"
        descripcion="Registro de la mercadería ingresada al depósito. Se actualiza automáticamente."
        acciones={<Boton variante="secundario" icono="sync" onClick={cargar}>Actualizar</Boton>}
      />
      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}
      <Criterio titulo="Cómo se generan" icono="hub">
        Al registrar la recepción de una reposición se publica el evento ReposicionRecibida. Inventario y Avisos lo
        consumen por separado desde el tópico de mensajería, así que un aviso puede aparecer unos segundos después.
      </Criterio>
      <Tarjeta className="p-6 md:p-8">
        {avisos.length === 0 ? (
          <Vacio icono="notifications" titulo="Todavía no hay avisos">
            Registrá la recepción de una reposición en <Link to="/admin/reposiciones?paso=seguimiento" className="text-primary underline">Reposiciones</Link>.
          </Vacio>
        ) : (
          <ol className="space-y-6 border-l border-outline-variant/50 pl-6">
            {avisos.map((n) => (
              <li key={n.id} className="relative">
                <span className="absolute -left-[31px] top-1.5 h-3 w-3 rounded-full bg-primary" aria-hidden="true" />
                <p className="font-label-sm text-label-sm uppercase text-primary">{fechaHora(n.creada)}</p>
                <p className="mt-1 font-body-lg text-body-lg">{n.mensaje}</p>
              </li>
            ))}
          </ol>
        )}
      </Tarjeta>
    </>
  );
}
