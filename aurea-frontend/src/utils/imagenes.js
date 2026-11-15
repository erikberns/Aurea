const PREDETERMINADA = "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop";

/** Imagen de respaldo por categoría cuando la joya no tiene fotografía cargada. */
export function imagenDeRespaldo(categoria) {
  switch (categoria) {
    case "Collares & Medallas":
    case "Pulseras & Eslabones":
      return "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop";
    case "Pendientes & Huggies":
      return "https://images.unsplash.com/photo-1630019852942-f89202989a59?q=80&w=800&auto=format&fit=crop";
    default:
      return PREDETERMINADA;
  }
}

export const imagenDeProducto = (producto) =>
  producto?.imageUrl || imagenDeRespaldo(producto?.category?.description);
