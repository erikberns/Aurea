package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;

/**
 * El proveedor respondió, pero rechazó la consulta por un dato del cliente (SOAP Fault con faultcode Client),
 * por ejemplo un código de producto que no existe en su catálogo. Es un error del pedido (400), no una caída
 * del proveedor (503): no abre el circuito ni sugiere reintentar.
 */
public class SolicitudRechazadaPorProveedorException extends IllegalArgumentException {
    public SolicitudRechazadaPorProveedorException(String mensaje) {
        super(mensaje);
    }
}
