package com.joyeriaEcommerce.AureaTPO.productos.negocio;

/**
 * Asistente de descripciones con IA. El negocio no arma llamadas al modelo: le pide al gateway,
 * valida la respuesta y, si algo falla, devuelve una plantilla (fallback a modo manual).
 */
public interface ISugerenciasProducto {

    SugerenciaProductoDTO sugerirDescripcion(DatosSugerenciaDTO datos, String usuario);

    EstadoAsistenteIADTO estado();
}
