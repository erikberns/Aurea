import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { catalogoService } from "../services/catalogoService";
import CatalogoView from "./CatalogoView";

export default function Catalogo() {
  // Estado local e interacción con APIs (Componente Stateful / Container)
  const [searchParams, setSearchParams] = useSearchParams();
  const categoriaFiltro = searchParams.get("categoria");
  const searchFiltro = searchParams.get("search");
  const sortFiltro = searchParams.get("sort") || "populares";
  
  const [joyas, setJoyas] = useState([]);
  const [categorias, setCategorias] = useState([]);

  useEffect(() => {
    catalogoService.buscarJoyas({ 
      categoria: categoriaFiltro || undefined,
      search: searchFiltro || undefined,
      sort: sortFiltro !== "populares" ? sortFiltro : undefined
    }).then(setJoyas);
    catalogoService.listarCategorias().then(setCategorias);
  }, [categoriaFiltro, searchFiltro, sortFiltro]);

  const setFiltro = (key, value) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    setSearchParams(params);
  };

  const clearFiltros = () => {
    setSearchParams(new URLSearchParams());
  };

  return (
    <CatalogoView 
      joyas={joyas} 
      categorias={categorias} 
      categoriaFiltro={categoriaFiltro}
      searchFiltro={searchFiltro}
      sortFiltro={sortFiltro}
      setFiltro={setFiltro}
      clearFiltros={clearFiltros}
    />
  );
}
