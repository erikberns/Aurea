// ---------------------------------------------------------------------------
// catalogoService.js
// Representa, del lado del frontend, al consumidor de iCatalogo
// (ServicioDeCatalogo). Es intencionalmente stateless: cada función recibe
// todo lo que necesita como parámetro y no depende de estado previo.
// Los datos están hardcodeados para la demo visual; en la integración real
// esto se reemplaza por llamadas httpRequest("/catalogo/...") iguales a las
// de usuariosService.js.
// ---------------------------------------------------------------------------

export const JOYAS = [
  {
    id: "j-001",
    categoria: "Anillos apilables",
    nombre: "Anillo Apilable 'Luna Nueva'",
    descripcion: "Plata de Ley 925 con circonitas engastadas a mano.",
    precio: 45000,
    badge: "Más Vendido",
    opciones: ["10", "12", "14", "16"],
    imagen:
      "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop",
  },
  {
    id: "j-002",
    categoria: "Collares & Medallas",
    nombre: "Collar Medalla 'Astro Solar'",
    descripcion: "Baño de Oro 18k con cadena ajustable (40–45 cm).",
    precio: 89000,
    badge: "Tendencia",
    opciones: ["Cadena 45 cm (Ajustable)"],
    imagen:
      "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop",
  },
  {
    id: "j-003",
    categoria: "Pulseras & Eslabones",
    nombre: "Pulsera Eslabones 'Aura Link'",
    descripcion: "Plata de Ley 925 rodiada antidesgaste de brillo espejo.",
    precio: 65000,
    badge: "Favorito",
    opciones: ["17 cm + 3 cm extensión"],
    imagen:
      "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop",
  },
  {
    id: "j-004",
    categoria: "Pendientes & Huggies",
    nombre: "Pendientes Aros 'Demi Huggies'",
    descripcion: "Plata 925 con baño de Oro 18k y cierre click seguro.",
    precio: 39000,
    badge: "Esenciales",
    opciones: ["12 mm (Mini aro)"],
    imagen:
      "https://images.unsplash.com/photo-1630019852942-f89202989a59?q=80&w=800&auto=format&fit=crop",
  },
  {
    id: "j-005",
    categoria: "Solitarios Fieles",
    nombre: "Anillo Solitario 'Eternity Sparkle'",
    descripcion: "Oro Vermeil 18k con gema central de topacio blanco natural.",
    precio: 120000,
    badge: "Especial Compromiso",
    opciones: ["12", "14", "16"],
    imagen:
      "https://images.unsplash.com/photo-1602751584547-4a80e5301045?q=80&w=800&auto=format&fit=crop",
  },
  {
    id: "j-006",
    categoria: "Perlas Barrocas & Mini",
    nombre: "Gargantilla Perlas 'Riviera'",
    descripcion: "Perlas cultivadas de agua dulce y broche marinero de plata dorada.",
    precio: 75000,
    badge: "Nueva Colección",
    opciones: ["42 cm Choker"],
    imagen:
      "https://images.unsplash.com/photo-1599459183200-59c7687a0275?q=80&w=800&auto=format&fit=crop",
  },
];

export const catalogoService = {
  buscarJoyas(filtros = {}) {
    let resultado = JOYAS;
    if (filtros.categoria) {
      resultado = resultado.filter((j) => j.categoria === filtros.categoria);
    }
    return Promise.resolve(resultado);
  },
  consultarJoya(joyaId) {
    return Promise.resolve(JOYAS.find((j) => j.id === joyaId) || null);
  },
  listarCategorias() {
    return Promise.resolve([...new Set(JOYAS.map((j) => j.categoria))]);
  },
};
