package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/**
 * Resultado del gateway: el valor ya interpretado y validado, o vacío con el motivo del fallback.
 *
 * @param aviso texto para mostrar al usuario cuando no hubo respuesta válida
 */
public record ResultadoIA<T>(T valor, ResultadoLlamadaIA resultado, String aviso) {

    public boolean exito() {
        return resultado == ResultadoLlamadaIA.OK && valor != null;
    }
}
