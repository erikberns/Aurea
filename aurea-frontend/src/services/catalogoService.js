import { getProductos, getProductoById } from "./productosService";
import { imagenDeRespaldo as getFallbackImage } from "../utils/imagenes";

// La tienda solo muestra joyas activas: las archivadas desde el panel quedan ocultas.
const activos = (productos) => productos.filter((p) => p.active !== false);

export const catalogoService = {
  async buscarJoyas(filtros = {}) {
    try {
      const productos = activos(await getProductos());
      
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
      if (filtros.sort) {
        if (filtros.sort === "asc") {
          resultado.sort((a, b) => a.precio - b.precio);
        } else if (filtros.sort === "desc") {
          resultado.sort((a, b) => b.precio - a.precio);
        }
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
      const productos = activos(await getProductos());
      const categorias = productos.map(p => p.category ? p.category.description : "Joyería");
      return [...new Set(categorias)];
    } catch (e) {
      console.error(e);
      return [];
    }
  },
};
