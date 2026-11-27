import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { catalogoService } from "../services/catalogoService";
import CatalogoView from "./CatalogoView";
import AureaLoader from "../components/AureaLoader";

export default function Catalogo() {
  // Estado local e interacción con APIs (Componente Stateful / Container)
  const [searchParams, setSearchParams] = useSearchParams();
  const categoriaFiltro = searchParams.get("categoria");
  const searchFiltro = searchParams.get("search");
  const sortFiltro = searchParams.get("sort") || "populares";
  const precioMinimo = searchParams.get("precioMin") || "";
  const precioMaximo = searchParams.get("precioMax") || "";
  const claveConsulta = JSON.stringify([categoriaFiltro, searchFiltro, sortFiltro]);
  const [resultado, setResultado] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);

  useEffect(() => {
    let activo = true;
    Promise.all([
      catalogoService.buscarJoyas({
        categoria: categoriaFiltro || undefined,
        search: searchFiltro || undefined,
        sort: sortFiltro !== "populares" ? sortFiltro : undefined
      }),
      catalogoService.listarCategorias()
    ])
      .then(([listaJoyas, listaCategorias]) => {
        if (!activo) return;
        setResultado({ claveConsulta, joyas: listaJoyas, categorias: listaCategorias });
        setErrorCarga(null);
      })
      .catch((e) => {
        if (activo) setErrorCarga({ claveConsulta, mensaje: e.message || "No se pudo cargar el catálogo." });
      });
    return () => { activo = false; };
  }, [categoriaFiltro, searchFiltro, sortFiltro, claveConsulta]);

  const cargando = resultado?.claveConsulta !== claveConsulta && errorCarga?.claveConsulta !== claveConsulta;
  if (cargando) return <AureaLoader text="Cargando catálogo..." fullScreen />;
  if (errorCarga?.claveConsulta === claveConsulta) {
    return <p role="alert" className="px-6 py-16 text-center text-error">{errorCarga.mensaje}</p>;
  }
  const joyasBase = resultado?.claveConsulta === claveConsulta ? resultado.joyas : [];
  const joyas = joyasBase.filter((joya) => {
    const precio = Number(joya.precio);
    return (!precioMinimo || precio >= Number(precioMinimo))
      && (!precioMaximo || precio <= Number(precioMaximo));
  });
  const categorias = resultado?.claveConsulta === claveConsulta ? resultado.categorias : [];

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
      precioMinimo={precioMinimo}
      precioMaximo={precioMaximo}
      setFiltro={setFiltro}
      clearFiltros={clearFiltros}
    />
  );
}
