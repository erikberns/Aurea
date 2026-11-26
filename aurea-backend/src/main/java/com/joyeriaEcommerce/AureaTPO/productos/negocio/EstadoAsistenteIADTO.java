package com.joyeriaEcommerce.AureaTPO.productos.negocio;

/** Si el asistente está encendido y con qué proveedor y modelo trabaja. */
public record EstadoAsistenteIADTO(boolean habilitada, String proveedor, String modelo) {
}
