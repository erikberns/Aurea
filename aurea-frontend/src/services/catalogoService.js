import { getProductos, getProductoById } from "./productosService";

const getFallbackImage = (categoria) => {
  switch (categoria) {
    case "Anillos apilables":
      return "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop";
    case "Collares & Medallas":
    case "Pulseras & Eslabones":
      return "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop";
    case "Pendientes & Huggies":
      return "https://images.unsplash.com/photo-1630019852942-f89202989a59?q=80&w=800&auto=format&fit=crop";
    default:
      return "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop";
  }
};

export const catalogoService = {
  async buscarJoyas(filtros = {}) {
    try {
      const productos = await getProductos();
      
      // Mapear productos del backend al formato del frontend
      let resultado = productos.map(p => {
        const categoria = p.category ? p.category.description : "Joyería";
        return {
          id: p.id,
          categoria,
          nombre: p.name,
          descripcion: p.description,
          precio: p.price,
          badge: "",
          opciones: ["Única"],
          imagen: p.imageUrl || getFallbackImage(categoria)
        };
      });

      if (filtros.categoria) {
        resultado = resultado.filter((j) => j.categoria === filtros.categoria);
      }
      if (filtros.search) {
        const s = filtros.search.toLowerCase();
        resultado = resultado.filter(j => 
          j.nombre.toLowerCase().includes(s) || 
          (j.descripcion && j.descripcion.toLowerCase().includes(s))
        );
      }
      return resultado;
    } catch (e) {
      console.error(e);
      return [];
    }
  },
  
  async consultarJoya(joyaId) {
    try {
      const p = await getProductoById(joyaId);
      const categoria = p.category ? p.category.description : "Joyería";
      return {
        id: p.id,
        categoria,
        nombre: p.name,
        descripcion: p.description,
        precio: p.price,
        badge: "",
        opciones: ["Única"],
        imagen: p.imageUrl || getFallbackImage(categoria)
      };
    } catch (e) {
      console.error(e);
      return null;
    }
  },
  
  async listarCategorias() {
    try {
      const productos = await getProductos();
      const categorias = productos.map(p => p.category ? p.category.description : "Joyería");
      return [...new Set(categorias)];
    } catch (e) {
      console.error(e);
      return [];
    }
  },
};
