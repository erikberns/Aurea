import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { catalogoService } from "../services/catalogoService";
import ProductCard from "../components/ProductCard";

const fmt = new Intl.NumberFormat("es-AR");

const CATEGORIAS = [
  {
    nombre: "Anillos Apilables & Solitarios",
    tag: "Plata & Oro",
    desde: 35000,
    filtro: "Anillos apilables",
    imagen:
      "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop",
  },
  {
    nombre: "Collares & Medallas",
    tag: "Medallas 18k",
    desde: 52000,
    filtro: "Collares & Medallas",
    imagen:
      "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop",
  },
  {
    nombre: "Pulseras & Eslabones",
    tag: "Plata 925",
    desde: 48000,
    filtro: "Pulseras & Eslabones",
    imagen:
      "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop",
  },
  {
    nombre: "Pendientes & Huggies",
    tag: "Huggies Cómodos",
    desde: 32000,
    filtro: "Pendientes & Huggies",
    imagen:
      "https://images.unsplash.com/photo-1630019852942-f89202989a59?q=80&w=800&auto=format&fit=crop",
  },
];

const TESTIMONIOS = [
  {
    iniciales: "MF",
    avatarCls: "bg-secondary-container text-on-secondary-container",
    texto:
      "Compré el collar Astro Solar y superó mis expectativas. El tono del baño de oro no es amarillo chillón, tiene esa calidez justa de joya fina. Llegó impecable a Palermo en 2 días.",
    nombre: "Mariana F.",
    ubicacion: "CABA, Buenos Aires",
  },
  {
    iniciales: "VG",
    avatarCls: "bg-primary-fixed text-on-primary-fixed",
    texto:
      "Tenía dudas con la medida del anillo Luna Nueva. La atención por WhatsApp fue impecable y me enviaron la plantilla de talles. Me calzó como un guante. Excelente experiencia de compra.",
    nombre: "Valeria G.",
    ubicacion: "Córdoba Capital",
  },
  {
    iniciales: "SL",
    avatarCls: "bg-tertiary-fixed text-on-tertiary-fixed",
    texto:
      "La pulsera Aura Link en plata 925 es sólida y no se engancha con nada. La uso a diario para ir a trabajar y entrenar, sigue reluciente. Destaco la rapidez de Andreani hasta Rosario.",
    nombre: "Sofía L.",
    ubicacion: "Rosario, Santa Fe",
  },
  {
    iniciales: "CR",
    avatarCls: "bg-secondary-fixed-dim text-on-secondary-fixed",
    texto:
      "El packaging de regalo es de otro nivel, parece una casa de alta gama europea pero con calidez argentina. Los Demi Huggies no me irritan la oreja para nada. Feliz con la compra.",
    nombre: "Camila R.",
    ubicacion: "Mendoza Capital",
  },
];

function Stars({ size = "text-sm" }) {
  return (
    <div className="flex text-primary">
      {Array.from({ length: 5 }).map((_, i) => (
        <span key={i} className={`material-symbols-outlined ${size}`} style={{ fontVariationSettings: "'FILL' 1" }}>
          star
        </span>
      ))}
    </div>
  );
}

export default function Landing() {
  const [destacados, setDestacados] = useState(null);

  useEffect(() => {
    let activo = true;
    catalogoService.buscarJoyas().then(joyas => {
      if (activo) setDestacados(joyas.slice(0, 4));
    });
    return () => { activo = false; };
  }, []);

  return (
    <>
      {/* HERO */}
      <section className="relative overflow-hidden pt-8 pb-16 md:py-20 lg:py-24 border-b border-outline-variant/30">
        <div className="max-w-7xl mx-auto px-6 md:px-12 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          <div className="lg:col-span-6 flex flex-col justify-center text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container border border-outline-variant/40 w-max mb-6">
              <span className="w-2 h-2 rounded-full bg-primary-container animate-pulse" />
              <span className="font-label-sm text-label-sm text-secondary tracking-wider">
                COLECCIÓN OTOÑO 2026 · BUENOS AIRES
              </span>
            </div>
            <h1 className="font-display-hero-mobile md:font-display-hero text-display-hero-mobile md:text-display-hero text-on-surface leading-[1.1] mb-6">
              Joyas para Vivir tu Día a Día con{" "}
              <span className="italic font-normal text-primary-container">Brillo Propio</span>.
            </h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant mb-8 max-w-xl leading-relaxed">
              Diseño contemporáneo en Plata de Ley 925 certificada y Baño de Oro 18k Vermeil.
              Hechas en Buenos Aires para perdurar sin perder su calidez natural.
            </p>
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 mb-10">
              <Link
                to="/catalogo"
                className="h-12 px-8 bg-primary-container hover:bg-[#8c6541] text-on-primary rounded-lg font-label-md text-label-md flex items-center justify-center gap-2 transition-all duration-200 active:scale-[0.99] shadow-sm"
              >
                <span>Explorar Catálogo</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </Link>
              <Link
                to="/catalogo"
                className="h-12 px-8 border border-primary-container text-secondary hover:bg-primary-container/10 rounded-lg font-label-md text-label-md flex items-center justify-center transition-all duration-200 active:scale-[0.99]"
              >
                Ver Novedades
              </Link>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-6 border-t border-outline-variant/40">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary text-xl">verified_user</span>
                <div>
                  <p className="font-label-md text-label-md text-on-surface">Garantía oficial 2 años</p>
                  <p className="font-body-sm text-body-sm text-outline">Cobertura total de fábrica</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary text-xl">recycling</span>
                <div>
                  <p className="font-label-md text-label-md text-on-surface">Metales 100% reciclados</p>
                  <p className="font-body-sm text-body-sm text-outline">Plata 925 y Vermeil 18k</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary text-xl">local_shipping</span>
                <div>
                  <p className="font-label-md text-label-md text-on-surface">Envíos a todo el país</p>
                  <p className="font-body-sm text-body-sm text-outline">Vía Andreani &amp; Correo Arg.</p>
                </div>
              </div>
            </div>
          </div>

          <div className="lg:col-span-6 relative">
            <div className="relative mx-auto max-w-lg lg:max-w-none">
              <div className="absolute -inset-3 rounded-2xl bg-gradient-to-tr from-primary-fixed/40 via-surface-container to-secondary-container/20 filter blur-xl opacity-70 -z-10" />
              <div className="grid grid-cols-12 gap-4 items-center">
                <div className="col-span-8 rounded-xl overflow-hidden bg-surface-container-lowest border border-outline-variant/30 shadow-[0_16px_40px_-8px_rgba(30,27,24,0.08)]">
                  <img
                    className="w-full h-[460px] object-cover hover:scale-105 transition-transform duration-700 ease-out"
                    alt="Mujer luciendo collares y anillos de oro vermeil y plata 925"
                    src="https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?q=80&w=800&auto=format&fit=crop"
                  />
                </div>
                <div className="col-span-4 flex flex-col gap-4">
                  <div className="rounded-lg overflow-hidden bg-surface-container-lowest border border-outline-variant/30 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)]">
                    <img
                      className="w-full h-48 object-cover hover:scale-105 transition-transform duration-500"
                      alt="Detalle macro de anillos apilables de oro vermeil"
                      src="https://images.unsplash.com/photo-1611955167811-4711904bb9f8?q=80&w=600&auto=format&fit=crop"
                    />
                  </div>
                  <div className="p-4 rounded-lg bg-surface-container-lowest/95 backdrop-blur-md border border-outline-variant/40 flex flex-col gap-1 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)]">
                    <div className="flex items-center gap-1.5 text-primary text-xs font-bold">
                      <span className="material-symbols-outlined text-sm" style={{ fontVariationSettings: "'FILL' 1" }}>
                        star
                      </span>
                      <span>4.9 / 5.0</span>
                    </div>
                    <p className="font-label-md text-label-md text-on-surface">Diseño de Origen</p>
                    <p className="font-body-sm text-body-sm text-on-surface-variant text-[11px] leading-tight">
                      Hecho en taller propio, San Telmo, Buenos Aires.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CATEGORÍAS */}
      <section className="py-16 md:py-24 bg-surface-container-low/40">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-12">
            <div>
              <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider block mb-2">
                Colecciones Básicas &amp; Esenciales
              </span>
              <h2 className="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface">
                Comprar por Categoría
              </h2>
            </div>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-md mt-3 md:mt-0">
              Piezas concebidas para combinarse con fluidez y acompañarte desde la mañana en la
              oficina hasta una cena especial.
            </p>
          </div>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
            {CATEGORIAS.map((cat) => (
              <Link
                key={cat.nombre}
                to={`/catalogo?categoria=${encodeURIComponent(cat.filtro)}`}
                className="group relative rounded-xl overflow-hidden bg-surface-container-lowest border border-outline-variant/30 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] flex flex-col"
              >
                <div className="aspect-[4/5] overflow-hidden relative">
                  <img
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    alt={cat.nombre}
                    src={cat.imagen}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-inverse-surface/60 via-transparent to-transparent opacity-60 group-hover:opacity-75 transition-opacity" />
                  <span className="absolute top-3 left-3 bg-surface-container-lowest/90 backdrop-blur-md px-2.5 py-1 rounded-full text-on-surface font-label-sm text-label-sm border border-outline-variant/40">
                    {cat.tag}
                  </span>
                </div>
                <div className="p-4 flex items-center justify-between">
                  <div>
                    <h3 className="font-headline-sm text-[18px] leading-snug text-on-surface group-hover:text-primary transition-colors">
                      {cat.nombre}
                    </h3>
                    <span className="font-body-sm text-body-sm text-outline">
                      Desde ${fmt.format(cat.desde)} ARS
                    </span>
                  </div>
                  <span className="material-symbols-outlined text-outline group-hover:text-primary group-hover:translate-x-1 transition-all">
                    arrow_forward
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* PIEZAS MÁS DESEADAS */}
      <section className="py-16 md:py-24" id="catalogo">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-10">
            <div>
              <div className="inline-flex items-center gap-1.5 text-primary text-label-sm font-label-sm uppercase tracking-wider mb-2">
                <span className="material-symbols-outlined text-base">auto_awesome</span>
                <span>Favoritos de Temporada</span>
              </div>
              <h2 className="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface">
                Piezas Más Deseadas
              </h2>
            </div>
            <span className="mt-4 sm:mt-0 text-body-sm font-body-sm text-on-surface-variant">
              Precios expresados en Pesos Argentinos ($ ARS)
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {destacados === null && <p>Cargando productos...</p>}
            {destacados?.length === 0 && <p>No hay productos disponibles para mostrar.</p>}
            {destacados?.map(joya => (
              <ProductCard key={joya.id} joya={joya} />
            ))}
          </div>
        </div>
      </section>

      {/* FILOSOFÍA / EL ARTE DE ÁUREA */}
      <section className="py-16 md:py-24 bg-surface-container/60 border-y border-outline-variant/30">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            <div className="lg:col-span-5 order-2 lg:order-1">
              <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider block mb-2">
                Trazabilidad &amp; Oficio
              </span>
              <h2 className="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface mb-6">
                El Arte de Áurea: Lujo Cercano y Honesto
              </h2>
              <p className="font-body-md text-body-md text-on-surface-variant mb-8 leading-relaxed">
                Desmitificamos la alta joyería eliminando los sobrecostos tradicionales. Cada pieza
                fusiona la precisión del diseño moderno con la destreza de orfebres independientes
                en Buenos Aires.
              </p>
              <div className="space-y-6">
                {[
                  {
                    icon: "layers",
                    titulo: "Vermeil 18k de 3 Micras",
                    texto:
                      "Una capa hasta 5 veces más gruesa que el enchapado común sobre base pura de Plata 925. No se despinta y cuida las pieles más sensibles.",
                  },
                  {
                    icon: "inventory_2",
                    titulo: "Packaging Sustentable Incluido",
                    texto:
                      "Caja rígida protectora de fibras recicladas con certificación FSC, bolsa de lino orgánico y paño pulidor anti-deslustre sin costo extra.",
                  },
                  {
                    icon: "handshake",
                    titulo: "Comercio Justo y Producción Ética",
                    texto:
                      "Metales recuperados y gemas éticas de origen certificado, apoyando el oficio artesanal local con salarios dignos.",
                  },
                ].map((item) => (
                  <div key={item.titulo} className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-lg bg-surface-container-lowest border border-outline-variant/40 flex items-center justify-center flex-shrink-0 text-primary">
                      <span className="material-symbols-outlined">{item.icon}</span>
                    </div>
                    <div>
                      <h4 className="font-title-md text-title-md text-on-surface mb-1">{item.titulo}</h4>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">{item.texto}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="lg:col-span-7 order-1 lg:order-2 grid grid-cols-2 gap-4">
              <div className="rounded-xl overflow-hidden shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] border border-outline-variant/40 pt-8">
                <img
                  className="w-full h-80 object-cover rounded-lg"
                  alt="Banco de orfebre en taller de Buenos Aires"
                  src="https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?q=80&w=700&auto=format&fit=crop"
                />
              </div>
              <div className="rounded-xl overflow-hidden shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] border border-outline-variant/40 pb-8">
                <img
                  className="w-full h-80 object-cover rounded-lg"
                  alt="Packaging sostenible de regalo Áurea"
                  src="https://images.unsplash.com/photo-1549062572-544a64fb0c56?q=80&w=700&auto=format&fit=crop"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* BENEFICIOS */}
      <section className="py-16 md:py-20">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider">
              Tu Compra 100% Protegida
            </span>
            <h2 className="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface mt-1">
              Experiencia de Compra Pensada para Argentina
            </h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              {
                icon: "credit_card",
                titulo: "Cuotas sin Interés",
                texto:
                  "Aboná en 3 y hasta 6 cuotas sin interés mediante Mercado Pago, MODO y tarjetas bancarias emitidas en Argentina (Visa, Mastercard, Cabal).",
                nota: "Pasarela encriptada SSL 256-bit",
                notaIcon: "verified",
              },
              {
                icon: "near_me",
                titulo: "Envíos con Seguimiento",
                texto:
                  "Despacho prioritario en 24h vía Andreani o Correo Argentino directo a domicilio o sucursal. Notificaciones por WhatsApp en cada tramo.",
                nota: "Seguro de traslado 100% incluido",
                notaIcon: "check_circle",
              },
              {
                icon: "published_with_changes",
                titulo: "Cambios Simples & Garantía",
                texto:
                  "Si la talla de anillo no es perfecta, el primer cambio de medida es totalmente gratuito dentro de los 30 días corridos. Garantía escrita de 2 años.",
                nota: "Atención local personalizada",
                notaIcon: "support_agent",
              },
            ].map((card) => (
              <div
                key={card.titulo}
                className="p-8 rounded-xl bg-surface-container-lowest border border-outline-variant/30 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] hover:border-primary-container/40 transition-all"
              >
                <div className="w-12 h-12 rounded-lg bg-primary-fixed flex items-center justify-center text-on-primary-fixed mb-5">
                  <span className="material-symbols-outlined text-2xl">{card.icon}</span>
                </div>
                <h3 className="font-headline-sm text-headline-sm text-on-surface mb-2">{card.titulo}</h3>
                <p className="font-body-md text-body-md text-on-surface-variant mb-4">{card.texto}</p>
                <div className="flex items-center gap-2 pt-2 text-xs font-semibold text-secondary">
                  <span className="material-symbols-outlined text-base">{card.notaIcon}</span>
                  <span>{card.nota}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* TESTIMONIOS */}
      <section className="py-16 md:py-24 bg-surface-container-low/60 border-t border-outline-variant/30">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-12">
            <div>
              <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider block mb-1">
                Experiencias Reales
              </span>
              <h2 className="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg text-on-surface">
                Comunidad Áurea en el País
              </h2>
            </div>
            <div className="mt-4 md:mt-0 flex items-center gap-2 bg-surface-container-lowest px-4 py-2 rounded-lg border border-outline-variant/40">
              <Stars size="text-lg" />
              <span className="font-label-md text-label-md text-on-surface">+1.850 clientas felices en 2026</span>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {TESTIMONIOS.map((t) => (
              <div
                key={t.nombre}
                className="p-6 rounded-xl bg-surface-container-lowest border border-outline-variant/30 shadow-[0_4px_20px_-2px_rgba(140,101,65,0.06)] flex flex-col justify-between"
              >
                <div>
                  <Stars />
                  <p className="font-body-md text-body-md text-on-surface italic mb-4 mt-3">"{t.texto}"</p>
                </div>
                <div className="pt-4 border-t border-outline-variant/20 flex items-center gap-3">
                  <div className={`w-8 h-8 rounded-full font-semibold flex items-center justify-center text-xs ${t.avatarCls}`}>
                    {t.iniciales}
                  </div>
                  <div>
                    <p className="font-label-md text-label-md text-on-surface">{t.nombre}</p>
                    <p className="font-body-sm text-body-sm text-outline">{t.ubicacion}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
