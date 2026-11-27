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

/**
 * Si una foto externa deja de existir (por ejemplo, un 404 de Unsplash), se reemplaza una sola vez
 * por la imagen predeterminada para que la tienda no muestre íconos rotos.
 * Escucha en fase de captura porque el evento "error" de las imágenes no burbujea.
 */
export function activarRespaldoDeImagenes() {
  window.addEventListener(
    "error",
    (evento) => {
      const img = evento.target;
      if (img instanceof HTMLImageElement && !img.dataset.respaldo && img.src !== PREDETERMINADA) {
        img.dataset.respaldo = "1";
        img.src = PREDETERMINADA;
      }
    },
    true
  );
}

export const imagenDeProducto = (producto) =>
  producto?.imageUrl || imagenDeRespaldo(producto?.category?.description);
