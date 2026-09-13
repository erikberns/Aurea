import { httpRequest } from "../api/httpClient";
export const getProductos = () => httpRequest("/productos");
export const getProductoById = id => httpRequest(`/productos/${id}`);
