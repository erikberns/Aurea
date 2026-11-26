package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/** Texto crudo que devolvió el modelo y los tokens consumidos (para trazas y costo). */
public record RespuestaModelo(String texto, int tokensEntrada, int tokensSalida) {
}
