package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/** Cómo terminó una llamada. Todo lo que no es OK lleva a la funcionalidad a su fallback. */
public enum ResultadoLlamadaIA {
    OK,
    APAGADA,
    CIRCUITO_ABIERTO,
    TIMEOUT,
    ERROR_MODELO,
    RESPUESTA_INVALIDA
}
