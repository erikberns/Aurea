import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { catalogoService } from "../services/catalogoService";
import CatalogoView from "./CatalogoView";

export default function Catalogo() {
  // Estado local e interacción con APIs (Componente Stateful / Container)
  const [searchParams] = useSearchParams();
  const categoriaFiltro = searchParams.get("categoria");
  const [joyas, setJoyas] = useState([]);
  const [categorias, setCategorias] = useState([]);

  useEffect(() => {
    catalogoService.buscarJoyas(categoriaFiltro ? { categoria: categoriaFiltro } : {}).then(setJoyas);
    catalogoService.listarCategorias().then(setCategorias);
  }, [categoriaFiltro]);

  // Se delega el 100% de la responsabilidad visual al componente Stateless
  return (
    <CatalogoView 
      joyas={joyas} 
      categorias={categorias} 
      categoriaFiltro={categoriaFiltro} 
    />
  );
}
