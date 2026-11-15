import { useCallback, useEffect, useMemo, useState } from "react";
import { adminService } from "../../services/adminService";
import { ESTADOS_PAGO, ESTADOS_PEDIDO, fecha, fechaHora, moneda } from "../../utils/admin";
import {
  Boton, Busqueda, Cargando, Chips, Encabezado, Estado, Indicador, Mensaje, Tarjeta, TituloSeccion, Vacio,
} from "../../components/admin/AdminUI";

/**
 * Pedidos de todos los clientes con el estado de su cobro (componente Pagos).
 * Permite aprobar la devolución de un pedido COMPLETED; si el pedido ya no admite esa
 * transición, el backend (patrón State) responde 409 y se muestra su mensaje.
 */
export default function AdminPedidos() {
  const [pedidos, setPedidos] = useState(null);
  const [pagos, setPagos] = useState([]);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState("");
  const [procesando, setProcesando] = useState(null);
  const [estado, setEstado] = useState("TODOS");
  const [busqueda, setBusqueda] = useState("");

  const cargar = useCallback(async () => {
    setError("");
    const [p, g] = await Promise.allSettled([adminService.pedidos(), adminService.pagos()]);
    if (p.status === "fulfilled") setPedidos([...p.value].sort((a, b) => b.id - a.id));
    else {
      setPedidos((actual) => actual ?? []);
      setError(p.reason?.message);
    }
    setPagos(g.status === "fulfilled" ? g.value : []);
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // La lista de pagos viene ordenada del más reciente al más antiguo: el primero es el vigente.
  const pagoPorOrden = useMemo(() => {
    const mapa = new Map();
    for (const pago of pagos) if (!mapa.has(pago.ordenId)) mapa.set(pago.ordenId, pago);
    return mapa;
  }, [pagos]);

  // Intentos de pago cuyo pedido no existe: la compra se revirtió (por ejemplo, tarjeta rechazada).
  const intentosSinPedido = useMemo(() => {
    const ids = new Set((pedidos ?? []).map((o) => o.id));
    return pagos.filter((pago) => !ids.has(pago.ordenId));
  }, [pagos, pedidos]);

  const aprobarDevolucion = async (orden) => {
    setError("");
    setAviso("");
    setProcesando(orden.id);
    try {
      const actualizado = await adminService.aprobarDevolucion(orden.id);
      setPedidos((lista) => lista.map((o) => (o.id === orden.id ? { ...o, ...actualizado } : o)));
      setAviso(`Devolución aprobada: el pedido #ORD-${orden.id} quedó cancelado.`);
    } catch (e) {
      setError(e.status === 409 ? `${e.message} (409 Conflict)` : e.message);
    } finally {
      setProcesando(null);
    }
  };

  const cuenta = (clave) => (pedidos ?? []).filter((o) => o.status === clave).length;

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return (pedidos ?? [])
      .filter((o) => estado === "TODOS" || o.status === estado)
      .filter((o) => {
        if (!texto) return true;
        const cliente = `${o.user?.nombre ?? ""} ${o.user?.apellido ?? ""} ${o.user?.email ?? ""}`.toLowerCase();
        return cliente.includes(texto) || String(o.id).includes(texto.replace(/\D/g, "") || "x");
      });
  }, [pedidos, estado, busqueda]);

  if (!pedidos) return <Cargando texto="Cargando pedidos…" />;

  const facturado = pedidos.filter((o) => o.status !== "CANCELLED").reduce((t, o) => t + (o.total ?? 0), 0);

  return (
    <>
      <Encabezado
        etiqueta="Flujo de venta"
        titulo="Pedidos de la tienda"
        descripcion="Seguimiento de los pedidos de todos los clientes y del cobro realizado por la pasarela de pago."
        acciones={<Boton variante="secundario" icono="sync" onClick={cargar}>Actualizar</Boton>}
      />
      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}
      {aviso && <Mensaje tono="exito" onCerrar={() => setAviso("")}>{aviso}</Mensaje>}

      <div className="mb-8 grid gap-5 md:grid-cols-3">
        <Indicador etiqueta="Pedidos registrados" icono="receipt_long" valor={pedidos.length}
          detalle={`${cuenta("CONFIRMED")} confirmados · ${cuenta("COMPLETED")} completados`} />
        <Indicador etiqueta="Facturación" icono="payments" tono="calido" valor={moneda(facturado)}
          detalle="Total de pedidos no cancelados (ARS)" />
        <Indicador etiqueta="Cobros a revisar" icono="credit_card_off" tono="alerta"
          alerta={pagos.some((p) => p.estado === "CAPTURA_PENDIENTE")}
          valor={pagos.filter((p) => p.estado === "CAPTURA_PENDIENTE").length}
          detalle="Pedidos confirmados con captura pendiente" />
      </div>

      <Tarjeta className="mb-6 flex flex-col gap-4 p-4 lg:flex-row lg:items-center lg:justify-between">
        <Busqueda valor={busqueda} onCambiar={setBusqueda} placeholder="Buscá por cliente, email o número" />
        <Chips etiqueta="Filtrar por estado" valor={estado} onCambiar={setEstado} opciones={[
          { valor: "TODOS", texto: "Todos", cantidad: pedidos.length },
          ...Object.entries(ESTADOS_PEDIDO).map(([clave, e]) => ({ valor: clave, texto: e.texto, cantidad: cuenta(clave) })),
        ]} />
      </Tarjeta>

      <Tarjeta className="overflow-hidden">
        {visibles.length === 0 ? (
          <Vacio icono="receipt_long" titulo="No hay pedidos para mostrar" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] text-left">
              <thead className="bg-surface-container-low font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">
                <tr>
                  <th className="px-6 py-4">Pedido</th>
                  <th className="px-4 py-4">Cliente</th>
                  <th className="px-4 py-4">Artículos</th>
                  <th className="px-4 py-4">Total</th>
                  <th className="px-4 py-4">Estado</th>
                  <th className="px-4 py-4">Pago</th>
                  <th className="px-6 py-4">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30">
                {visibles.map((o) => {
                  const est = ESTADOS_PEDIDO[o.status] ?? { texto: o.status, tono: "neutro" };
                  const pago = pagoPorOrden.get(o.id);
                  const estPago = pago ? ESTADOS_PAGO[pago.estado] ?? { texto: pago.estado, tono: "neutro" } : null;
                  return (
                    <tr key={o.id} className="align-top">
                      <td className="px-6 py-4">
                        <p className="font-label-md text-label-md">#ORD-{o.id}</p>
                        <p className="font-body-sm text-body-sm text-on-surface-variant">{fecha(o.orderDate)}</p>
                      </td>
                      <td className="px-4 py-4">
                        <p>{o.user ? `${o.user.nombre} ${o.user.apellido}` : "—"}</p>
                        <p className="font-body-sm text-body-sm text-on-surface-variant">{o.user?.email}</p>
                        <p className="max-w-xs truncate font-body-sm text-body-sm text-on-surface-variant">{o.shippingAddress}</p>
                      </td>
                      <td className="px-4 py-4">
                        <ul className="space-y-0.5 font-body-sm text-body-sm">
                          {(o.items ?? []).map((i) => (
                            <li key={i.id}>{i.quantity} × {i.product?.name ?? `Producto #${i.product?.id}`}</li>
                          ))}
                        </ul>
                      </td>
                      <td className="px-4 py-4 font-title-md text-title-md text-primary">{moneda(o.total)}</td>
                      <td className="px-4 py-4"><Estado tono={est.tono}>{est.texto}</Estado></td>
                      <td className="px-4 py-4">
                        {estPago ? (
                          <>
                            <Estado tono={estPago.tono}>{estPago.texto}</Estado>
                            <p className="mt-1 font-body-sm text-body-sm text-on-surface-variant">
                              {pago.proveedor === "stripe" ? "Stripe (modo prueba)" : "Pasarela simulada"}
                            </p>
                          </>
                        ) : (
                          <span className="font-body-sm text-body-sm text-on-surface-variant">Sin cobro registrado</span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {o.status === "COMPLETED" ? (
                          <Boton variante="secundario" icono="undo" disabled={procesando === o.id}
                            onClick={() => aprobarDevolucion(o)}>
                            Aprobar devolución
                          </Boton>
                        ) : (
                          <span className="font-body-sm text-body-sm text-on-surface-variant">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Tarjeta>

      <div className="mt-10">
        <TituloSeccion etiqueta="Pasarela de pago" titulo="Intentos de pago sin pedido" />
        <p className="mb-4 font-body-md text-body-md text-on-surface-variant">
          Cobros que no llegaron a generar un pedido: la compra se revirtió completa y no quedó nada cobrado.
        </p>
        <Tarjeta className="overflow-hidden">
          {intentosSinPedido.length === 0 ? (
            <Vacio icono="credit_card" titulo="No hay intentos de pago rechazados" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left">
                <thead className="bg-surface-container-low font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">
                  <tr>
                    <th className="px-6 py-4">Fecha</th>
                    <th className="px-4 py-4">Medio de prueba</th>
                    <th className="px-4 py-4">Monto</th>
                    <th className="px-4 py-4">Estado</th>
                    <th className="px-6 py-4">Detalle</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/30">
                  {intentosSinPedido.map((pago) => {
                    const est = ESTADOS_PAGO[pago.estado] ?? { texto: pago.estado, tono: "neutro" };
                    return (
                      <tr key={pago.id}>
                        <td className="px-6 py-4 font-body-sm text-body-sm">{fechaHora(pago.creado)}</td>
                        <td className="px-4 py-4 font-body-sm text-body-sm">{String(pago.metodo ?? "").replaceAll("_", " ").toLowerCase()}</td>
                        <td className="px-4 py-4">{moneda((pago.montoCentavos ?? 0) / 100)}</td>
                        <td className="px-4 py-4"><Estado tono={est.tono}>{est.texto}</Estado></td>
                        <td className="px-6 py-4 font-body-sm text-body-sm text-on-surface-variant">{pago.detalle ?? "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Tarjeta>
      </div>
    </>
  );
}
