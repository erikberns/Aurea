import ProductCard from "../components/ProductCard";

export default function CatalogoView({ joyas, categorias, categoriaFiltro, searchFiltro, sortFiltro, precioMinimo, precioMaximo, setFiltro, clearFiltros }) {
  return (
    <>
      <section className="w-full bg-surface-container-low border-b border-outline-variant/30 py-10 lg:py-14">
        <div className="w-full px-6 lg:px-16 mx-auto">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
            <div>
              <span className="font-label-sm text-label-sm uppercase tracking-[0.2em] text-primary font-semibold block mb-2">
                Servicio De Catálogo • Joyas de Uso Diario
              </span>
              <h1 className="font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface max-w-3xl leading-tight">
                Joyería Fina para Cada Día
              </h1>
              <p className="font-body-lg text-body-lg text-on-surface-variant mt-3 max-w-2xl">
                Piezas atemporales en Plata de Ley 925, Baño de Oro 18k y gemas minerales pensadas
                para llevar a diario o regalar momentos especiales.
              </p>
            </div>
            <div className="bg-surface-container-lowest border border-outline-variant/60 px-4 py-2 flex items-center gap-2.5 shadow-sm rounded-sm">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-600 animate-pulse" />
              <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface">
                Envío Gratis Disponible
              </span>
            </div>
          </div>
        </div>
      </section>

      <div className="w-full px-6 lg:px-16 mx-auto py-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          <aside className="lg:col-span-3 space-y-6 pb-8 lg:pb-0 lg:pr-6 border-b lg:border-b-0 lg:border-r border-outline-variant/30">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/40">
              <h2 className="font-headline-sm text-lg font-semibold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">tune</span>
                Filtrar Colección
              </h2>
              <button
              type="button"
              onClick={clearFiltros}
              className="font-label-sm text-label-sm text-primary uppercase hover:underline cursor-pointer"
              aria-label="Limpiar todos los filtros"
              >
                Limpiar
                </button>
            </div>
            <div>
              <h3 className="font-label-md text-label-md uppercase tracking-wider text-on-surface-variant mb-3">
                Categoría
              </h3>

              <ul className="space-y-2 font-body-md text-body-md text-on-surface">
                {categorias.map((c) => {
                  const seleccionada = categoriaFiltro === c;

                  return (
                    <li key={c}>
                      <button
                        type="button"
                        onClick={() => {
                          setFiltro("categoria", seleccionada ? null : c);
                        }}
                        aria-pressed={seleccionada}
                        className={`w-full flex items-center gap-2 text-left transition-colors ${
                          seleccionada
                            ? "text-primary font-semibold"
                            : "hover:text-primary"
                        }`}
                      >
                        <span
                          className={`w-3.5 h-3.5 shrink-0 rounded-sm border ${
                            seleccionada
                              ? "bg-primary-container border-primary-container"
                              : "border-outline-variant"
                          }`}
                        />

                        {c}
                      </button>
                    </li>
                  );
                })}
              </ul>

              {categoriaFiltro && (
                <div className="mt-4 p-3 bg-surface-container-low rounded-md">
                  <p className="font-label-sm text-label-sm text-on-surface-variant mb-2">
                    Categoría seleccionada:
                  </p>

                  <div className="flex items-center justify-between gap-2">
                    <span className="font-body-sm text-body-sm text-primary font-semibold">
                      {categoriaFiltro}
                    </span>

                    <button
                      type="button"
                      onClick={() => setFiltro("categoria", null)}
                      className="text-primary hover:underline text-sm"
                      aria-label="Quitar filtro de categoría"
                    >
                      Quitar
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="border-t border-outline-variant/40 pt-5">
              <h3 className="font-label-md text-label-md uppercase tracking-wider text-on-surface-variant mb-3">
                Rango de precios
              </h3>
              <div className="grid grid-cols-2 gap-3">
                <label className="font-label-sm text-label-sm text-on-surface-variant">
                  Desde
                  <span className="relative mt-1 block">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2">$</span>
                    <input type="number" min="0" step="5000" inputMode="numeric" value={precioMinimo} onChange={(event) => setFiltro("precioMin", event.target.value)} placeholder="Min." aria-label="Precio mínimo" className="w-full rounded border border-outline-variant bg-surface-container-lowest py-2 pl-7 pr-2 text-body-sm text-on-surface outline-none focus:border-primary" />
                  </span>
                </label>
                <label className="font-label-sm text-label-sm text-on-surface-variant">
                  Hasta
                  <span className="relative mt-1 block">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2">$</span>
                    <input type="number" min="0" step="5000" inputMode="numeric" value={precioMaximo} onChange={(event) => setFiltro("precioMax", event.target.value)} placeholder="Max." aria-label="Precio máximo" className="w-full rounded border border-outline-variant bg-surface-container-lowest py-2 pl-7 pr-2 text-body-sm text-on-surface outline-none focus:border-primary" />
                  </span>
                </label>
              </div>
              {precioMinimo && precioMaximo && Number(precioMinimo) > Number(precioMaximo) && (
                <p className="mt-2 text-body-sm text-error" role="alert">El precio mínimo supera al máximo.</p>
              )}
            </div>

          </aside>

          <div className="lg:col-span-9">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
              <div>
                <p className="font-body-md text-body-md text-on-surface-variant">
                  Mostrando <strong>{joyas.length}</strong> piezas en catálogo
                </p>
                {searchFiltro && (
                  <p className="font-body-sm text-body-sm text-primary mt-1">
                    Resultados para: <strong>"{searchFiltro}"</strong>
                    <button 
                      onClick={() => setFiltro("search", null)}
                      className="ml-2 text-on-surface-variant hover:text-error underline"
                    >
                      (quitar)
                    </button>
                  </p>
                )}
              </div>
              <select 
                className="border border-outline-variant rounded px-3 py-1.5 font-body-sm text-body-sm bg-surface-container-lowest"
                value={sortFiltro}
                onChange={(e) => setFiltro("sort", e.target.value)}
              >
                <option value="populares">Los más deseados</option>
                <option value="asc">Precio: menor a mayor</option>
                <option value="desc">Precio: mayor a menor</option>
              </select>
            </div>
            {joyas.length > 0 ? (
  <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
    {joyas.map((joya) => (
      <ProductCard key={joya.id} joya={joya} />
    ))}
  </div>
) : (
  <div className="flex flex-col items-center justify-center text-center py-16 px-6 bg-surface-container-low rounded-lg border border-outline-variant/30">
    <span className="material-symbols-outlined text-5xl text-primary mb-4">
      search_off
    </span>

    <h2 className="font-headline-sm text-xl text-on-surface mb-2">
      No encontramos joyas
    </h2>

    <p className="font-body-md text-on-surface-variant max-w-md mb-6">
      No hay productos que coincidan con tu búsqueda o los filtros seleccionados.
      Probá modificando los criterios o explorando otras categorías.
    </p>

    <button
      type="button"
      onClick={clearFiltros}
      className="px-6 py-2.5 bg-primary text-on-primary rounded-md font-label-md hover:opacity-90 transition-opacity"
    >
      Ver todas las joyas
    </button>
  </div>
)}
          </div>
        </div>
      </div>
    </>
  );
}
