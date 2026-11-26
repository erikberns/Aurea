import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { adminService } from "../../services/adminService";
import { imagenDeRespaldo } from "../../utils/imagenes";
import { UMBRAL_STOCK_CRITICO, moneda } from "../../utils/admin";
import { Boton, Cargando, Encabezado, Estado, Mensaje, Tarjeta, campo } from "../../components/admin/AdminUI";
import AsistenteDescripcionIA from "../../components/admin/AsistenteDescripcionIA";

const VACIO = { name: "", description: "", imageUrl: "", categoryId: "", price: "", stock: "", activo: true };

function Campo({ etiqueta, requerido, error, ayuda, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 flex items-center justify-between font-label-md text-label-md text-on-surface">
        <span>
          {etiqueta} {requerido && <span className="text-error">*</span>}
        </span>
      </span>
      {children}
      {error ? (
        <span className="mt-1 block font-body-sm text-body-sm text-error">{error}</span>
      ) : (
        ayuda && <span className="mt-1 block font-body-sm text-body-sm text-on-surface-variant">{ayuda}</span>
      )}
    </label>
  );
}

function validar(f, esNueva) {
  const errores = {};
  if (!f.name.trim()) errores.name = "Ingresá el nombre de la pieza.";
  if (esNueva && !f.categoryId) errores.categoryId = "Elegí una categoría.";
  const precio = Number(f.price);
  if (f.price === "" || !Number.isFinite(precio) || precio <= 0) errores.price = "Ingresá un precio mayor a cero.";
  const stock = Number(f.stock);
  if (f.stock === "" || !Number.isInteger(stock) || stock < 0) errores.stock = "Ingresá un número entero, 0 o mayor.";
  if (f.imageUrl.trim() && !/^https?:\/\/\S+$/i.test(f.imageUrl.trim())) errores.imageUrl = "Usá una URL que empiece con http:// o https://.";
  return errores;
}

/** Alta y edición de joyas. Solo el rol ADMIN llega a esta pantalla y el backend lo vuelve a exigir. */
export default function AdminProductoForm() {
  const { id } = useParams();
  const esNueva = !id;
  const navigate = useNavigate();
  const [form, setForm] = useState(VACIO);
  const [original, setOriginal] = useState(null);
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState("");
  const [imagenRota, setImagenRota] = useState(false);

  useEffect(() => {
    let activo = true;
    Promise.all([adminService.categorias(), esNueva ? null : adminService.producto(id)])
      .then(([c, p]) => {
        if (!activo) return;
        setCategorias(c);
        if (p) {
          setOriginal(p);
          setForm({
            name: p.name ?? "",
            description: p.description ?? "",
            imageUrl: p.imageUrl ?? "",
            categoryId: p.category?.id ? String(p.category.id) : "",
            price: String(p.price ?? ""),
            stock: String(p.stock ?? ""),
            activo: p.active !== false,
          });
        }
      })
      .catch((e) => activo && setError(e.message))
      .finally(() => activo && setCargando(false));
    return () => {
      activo = false;
    };
  }, [id, esNueva]);

  const actualizar = (campoNombre) => (e) => {
    const valor = e.target.value;
    setForm((f) => ({ ...f, [campoNombre]: valor }));
    setErrores((er) => ({ ...er, [campoNombre]: undefined }));
    if (campoNombre === "imageUrl") setImagenRota(false);
  };

  // El administrador decide: la sugerencia solo se copia al formulario cuando la acepta, y la puede editar.
  const usarSugerencia = (sugerencia) => {
    setForm((f) => ({
      ...f,
      description: sugerencia.descripcion,
      categoryId: sugerencia.categoriaId ? String(sugerencia.categoriaId) : f.categoryId,
    }));
    setErrores((er) => ({ ...er, categoryId: undefined }));
  };

  const categoriaElegida = categorias.find((c) => String(c.id) === form.categoryId);
  const vistaPrevia = useMemo(
    () => (!imagenRota && form.imageUrl.trim()) || imagenDeRespaldo(categoriaElegida?.description),
    [form.imageUrl, imagenRota, categoriaElegida]
  );

  const guardar = async (e) => {
    e.preventDefault();
    const encontrados = validar(form, esNueva);
    setErrores(encontrados);
    if (Object.keys(encontrados).length) return;
    setGuardando(true);
    setError("");
    const datos = {
      name: form.name.trim(),
      description: form.description.trim(),
      imageUrl: form.imageUrl.trim(),
      categoryId: form.categoryId ? Number(form.categoryId) : null,
    };
    try {
      if (esNueva) {
        const creada = await adminService.crearProducto({ ...datos, price: Number(form.price), stock: Number(form.stock) });
        if (!form.activo) await adminService.cambiarEstado(creada.id, false);
        navigate("/admin/productos", {
          replace: true,
          state: { mensaje: `"${creada.name}" se registró ${form.activo ? "y ya está visible en la tienda" : "como borrador interno"}.` },
        });
        return;
      }
      // Cada dato se actualiza con su recurso REST: PUT para la ficha, PATCH para precio, stock y estado.
      await adminService.actualizarProducto(id, datos);
      if (Number(form.price) !== original.price) await adminService.actualizarPrecio(id, Number(form.price));
      if (Number(form.stock) !== original.stock) await adminService.actualizarStock(id, Number(form.stock));
      if (form.activo !== (original.active !== false)) await adminService.cambiarEstado(id, form.activo);
      navigate("/admin/productos", { replace: true, state: { mensaje: `Cambios de "${datos.name}" guardados.` } });
    } catch (err) {
      setError(err.message);
      setGuardando(false);
    }
  };

  if (cargando) return <Cargando texto="Cargando ficha…" />;
  if (!esNueva && !original) {
    return (
      <>
        <Mensaje>{error || "No se encontró la joya."}</Mensaje>
        <Link to="/admin/productos" className="text-primary hover:underline">← Volver al catálogo</Link>
      </>
    );
  }

  const umbralSuperado = form.stock !== "" && Number(form.stock) <= UMBRAL_STOCK_CRITICO;

  return (
    <form onSubmit={guardar} noValidate>
      <div className="mb-4 flex items-center justify-between gap-4">
        <nav aria-label="Ruta" className="font-label-md text-label-md text-on-surface-variant">
          <Link to="/admin/productos" className="hover:text-primary">Productos</Link>
          <span className="mx-2 text-outline">/</span>
          <span className="text-primary">{esNueva ? "Nueva joya" : original.name}</span>
        </nav>
        <Link to="/admin/productos" className="flex items-center gap-1 rounded bg-surface-container px-3 py-1.5 font-label-md text-label-md text-primary hover:bg-surface-container-high">
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Volver al catálogo
        </Link>
      </div>

      <Encabezado
        etiqueta={esNueva ? "Taller orfebre · registro oficial" : `Ficha de joya · #${original.id}`}
        titulo={esNueva ? "Registrar nueva pieza de joyería" : "Editar pieza de joyería"}
        descripcion={
          esNueva
            ? "Completá los datos, el precio en ARS y las existencias iniciales para publicar la joya en la tienda."
            : "Los cambios se reflejan en la tienda en cuanto los guardás."
        }
      />

      {error && <Mensaje onCerrar={() => setError("")}>{error}</Mensaje>}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <div className="space-y-6">
          <Tarjeta className="p-6">
            <h2 className="mb-1 font-title-md text-title-md">Fotografía de la joya</h2>
            <p className="mb-4 font-body-sm text-body-sm text-on-surface-variant">
              Luz natural y fondo neutro. Si no cargás una, se usa una imagen de la categoría.
            </p>
            <Campo etiqueta="URL de la fotografía" error={errores.imageUrl}>
              <input type="url" value={form.imageUrl} onChange={actualizar("imageUrl")} className={campo}
                placeholder="https://…" aria-invalid={!!errores.imageUrl} />
            </Campo>
          </Tarjeta>

          <Tarjeta className="p-6">
            <h2 className="mb-1 font-title-md text-title-md">Estado de publicación</h2>
            <p className="mb-4 font-body-sm text-body-sm text-on-surface-variant">
              Definí si la pieza se ve en la tienda o queda archivada como borrador interno.
            </p>
            <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Estado de publicación">
              {[
                { valor: true, titulo: "Visible en tienda", detalle: "Lista para la venta", icono: "storefront" },
                { valor: false, titulo: "Borrador interno", detalle: "Oculta al público", icono: "drafts" },
              ].map((op) => (
                <button key={op.titulo} type="button" role="radio" aria-checked={form.activo === op.valor}
                  onClick={() => setForm((f) => ({ ...f, activo: op.valor }))}
                  className={`flex flex-col items-start gap-1 rounded-lg p-4 text-left transition-colors ${
                    form.activo === op.valor ? "bg-primary-container text-on-primary" : "bg-surface-container text-on-surface hover:bg-surface-container-high"
                  }`}>
                  <span className="material-symbols-outlined">{op.icono}</span>
                  <span className="font-label-md text-label-md">{op.titulo}</span>
                  <span className="font-body-sm text-body-sm opacity-80">{op.detalle}</span>
                </button>
              ))}
            </div>
          </Tarjeta>

          <Tarjeta className="p-6">
            <p className="mb-4 flex items-center justify-between font-label-md text-label-md">
              Vista previa en la tienda
              <span className="font-label-sm text-label-sm uppercase text-primary">Tienda Áurea</span>
            </p>
            <div className="mx-auto max-w-xs overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm">
              <img src={vistaPrevia} alt="" onError={() => setImagenRota(true)} className="aspect-[4/5] w-full object-cover" />
              <div className="p-4">
                <p className="font-label-sm text-label-sm uppercase text-on-surface-variant">
                  {categoriaElegida?.description ?? "Categoría"}
                </p>
                <p className="mt-1 line-clamp-1 font-headline-sm text-headline-sm">{form.name || "Nombre de la pieza"}</p>
                <p className="mt-3 rounded bg-surface-container px-3 py-2 font-title-md text-title-md text-primary">
                  {form.price ? `${moneda(form.price)} ARS` : "$ —"}
                </p>
                {!form.activo && <p className="mt-2"><Estado>Borrador · oculta</Estado></p>}
              </div>
            </div>
          </Tarjeta>
        </div>

        <div className="space-y-6">
          <Tarjeta className="space-y-5 p-6">
            <h2 className="flex items-center gap-2 font-headline-sm text-headline-sm">
              <span className="material-symbols-outlined text-primary">auto_awesome</span>
              Datos esenciales de la joya
            </h2>
            <Campo etiqueta="Nombre de la pieza" requerido error={errores.name}>
              <input value={form.name} onChange={actualizar("name")} className={campo} maxLength={255}
                placeholder="Collar Constelación Austral en Oro 18k" aria-invalid={!!errores.name} />
            </Campo>
            <Campo etiqueta="Categoría de catálogo" requerido={esNueva} error={errores.categoryId}>
              <select value={form.categoryId} onChange={actualizar("categoryId")} className={campo} aria-invalid={!!errores.categoryId}>
                <option value="">{esNueva ? "Elegí una categoría" : "Sin categoría"}</option>
                {categorias.map((c) => <option key={c.id} value={c.id}>{c.description}</option>)}
              </select>
            </Campo>
            <Campo etiqueta="Descripción artesanal" ayuda="Material, terminación y concepto de la pieza. Se muestra en la ficha de la tienda.">
              <textarea value={form.description} onChange={actualizar("description")} rows={5} maxLength={255}
                className={`${campo} h-auto py-3`} placeholder="Vermeil 18k sobre plata 925, medallón grabado a mano…" />
            </Campo>
            <AsistenteDescripcionIA nombre={form.name} onUsar={usarSugerencia} />
          </Tarjeta>

          <Tarjeta className="space-y-5 p-6">
            <h2 className="flex items-center gap-2 font-headline-sm text-headline-sm">
              <span className="material-symbols-outlined text-primary">payments</span>
              Valuación e inventario
            </h2>
            <div className="grid gap-5 sm:grid-cols-2">
              <Campo etiqueta="Precio al público (ARS)" requerido error={errores.price} ayuda="Con IVA incluido.">
                <input type="number" min="1" step="any" value={form.price} onChange={actualizar("price")} className={campo}
                  placeholder="115000" aria-invalid={!!errores.price} />
              </Campo>
              <Campo etiqueta="Stock en depósito" requerido error={errores.stock} ayuda="Unidades terminadas disponibles.">
                <input type="number" min="0" step="1" value={form.stock} onChange={actualizar("stock")} className={campo}
                  placeholder="10" aria-invalid={!!errores.stock} />
              </Campo>
            </div>
            {umbralSuperado && (
              <p className="flex items-start gap-2 rounded-lg bg-error-container/60 p-3 font-body-sm text-body-sm text-on-error-container">
                <span className="material-symbols-outlined text-[18px]">info</span>
                Con {form.stock} unidades la pieza queda en stock crítico (≤ {UMBRAL_STOCK_CRITICO}) y aparecerá en las alertas del panel.
                Podés reponerla desde Reposiciones.
              </p>
            )}
          </Tarjeta>
        </div>
      </div>

      <Tarjeta className="sticky bottom-4 z-10 mt-8 flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="flex items-center gap-2 font-body-sm text-body-sm text-on-surface-variant">
          <span className="material-symbols-outlined text-primary">verified</span>
          {form.activo ? "Al guardar, la pieza quedará visible en la tienda." : "Al guardar, la pieza quedará oculta al público."}
        </p>
        <div className="flex gap-3">
          <Boton variante="secundario" icono="close" onClick={() => navigate("/admin/productos")} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton type="submit" icono="check" disabled={guardando}>
            {guardando ? "Guardando…" : !esNueva ? "Guardar cambios" : form.activo ? "Guardar y publicar" : "Guardar borrador"}
          </Boton>
        </div>
      </Tarjeta>
    </form>
  );
}
