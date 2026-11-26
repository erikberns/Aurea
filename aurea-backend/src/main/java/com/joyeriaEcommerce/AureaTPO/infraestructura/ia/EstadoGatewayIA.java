package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/** Estado visible del gateway: si está encendido, con qué proveedor y modelo, y el estado del circuito. */
public record EstadoGatewayIA(boolean habilitada, String proveedor, String modelo, String circuito) {
}
