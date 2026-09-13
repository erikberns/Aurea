import { getProductos, getProductoById } from "./productosService";

export const catalogoService = {
  async buscarJoyas(filtros = {}) {
    try {
      const productos = await getProductos();
      
      // Mapear productos del backend al formato del frontend
      let resultado = productos.map(p => ({
        id: p.id,
        categoria: p.category ? p.category.description : "Joyería",
        nombre: p.name,
        descripcion: p.description,
        precio: p.discountPrice ?? p.price,
        badge: p.discountPrice ? "En Oferta" : "",
        opciones: ["Única"],
        imagen: p.imageUrl || "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop"
      }));

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
      return {
        id: p.id,
        categoria: p.category ? p.category.description : "Joyería",
        nombre: p.name,
        descripcion: p.description,
        precio: p.discountPrice ?? p.price,
        badge: p.discountPrice ? "En Oferta" : "",
        opciones: ["Única"],
        imagen: p.imageUrl || "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop"
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
