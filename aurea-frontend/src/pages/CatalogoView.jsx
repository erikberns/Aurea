import ProductCard from "../components/ProductCard";

export default function CatalogoView({ joyas, categorias, categoriaFiltro, searchFiltro, sortFiltro, setFiltro, clearFiltros }) {
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
              <span 
                className="font-label-sm text-label-sm text-primary uppercase cursor-pointer hover:underline"
                onClick={clearFiltros}
              >
                Limpiar
              </span>
            </div>
            <div>
              <h3 className="font-label-md text-label-md uppercase tracking-wider text-on-surface-variant mb-3">
                Categoría
              </h3>
              <ul className="space-y-2 font-body-md text-body-md text-on-surface">
                {categorias.map((c) => (
                  <li 
                    key={c} 
                    className="flex items-center gap-2 cursor-pointer hover:text-primary transition-colors"
                    onClick={() => {
                      if (categoriaFiltro === c) {
                        setFiltro("categoria", null);
                      } else {
                        setFiltro("categoria", c);
                      }
                    }}
                  >
                    <span
                      className={`w-3.5 h-3.5 rounded-sm border ${
                        categoriaFiltro === c ? "bg-primary-container border-primary-container" : "border-outline-variant"
                      }`}
                    />
                    {c}
                  </li>
                ))}
              </ul>
            </div>
            <div className="bg-surface-container rounded p-4">
              <p className="font-label-md text-label-md text-on-surface flex items-center gap-2 mb-2">
                <span className="material-symbols-outlined text-base">card_giftcard</span>
                ¿Necesitás un regalo?
              </p>
              <p className="font-body-sm text-body-sm text-on-surface-variant mb-3">
                Te enviamos tu joya en nuestra icónica caja con lazo y dedicatoria personalizada gratis.
              </p>
              <button className="w-full bg-surface-container-lowest border border-outline-variant py-2 font-label-sm text-label-sm uppercase tracking-wider">
                Descubrir Guía de Regalos
              </button>
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
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {joyas.map((joya) => (
                <ProductCard key={joya.id} joya={joya} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
