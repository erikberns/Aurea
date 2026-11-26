package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/**
 * Un pedido al gateway.
 *
 * @param funcionalidad nombre de la funcionalidad que llama (queda en la traza)
 * @param versionPrompt versión del prompt usado, por ejemplo descripcion-producto.v1
 * @param usuario       quien pide, para el límite de solicitudes (no se guarda en la traza)
 */
public record PedidoIA(String funcionalidad, String versionPrompt, String usuario, SolicitudModelo solicitud) {
}
