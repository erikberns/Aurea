package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/**
 * Lo que se le pide al modelo. Las instrucciones vienen del prompt versionado; la entrada lleva
 * los datos del usuario delimitados, para que el modelo los trate como datos y no como órdenes.
 */
public record SolicitudModelo(String instrucciones, String entrada, int maxTokensSalida, double temperatura) {
}
