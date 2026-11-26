package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/**
 * Puerto hacia un modelo de lenguaje (Adapter). Solo lo usa el {@link IAGateway}: el negocio
 * nunca conoce el proveedor, el SDK ni el protocolo. Cambiar de modelo es otra implementación.
 */
public interface ProveedorModeloIA {

    RespuestaModelo generar(SolicitudModelo solicitud);

    default String nombre() {
        return getClass().getSimpleName();
    }

    default String modelo() {
        return "-";
    }
}
