package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

/** Respuesta normalizada de la pasarela, independiente del proveedor. */
public record ResultadoPasarelaDTO(boolean aprobado, String referencia, String estadoExterno, String detalle) {}
