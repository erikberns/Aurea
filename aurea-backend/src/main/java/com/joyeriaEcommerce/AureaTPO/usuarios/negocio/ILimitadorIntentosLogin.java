package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

/**
 * Limita los intentos fallidos de inicio de sesión para frenar ataques de fuerza bruta.
 * Cuenta los fallos por email y por origen (IP); al superar el límite, el login responde 429
 * hasta que termina la ventana de tiempo.
 */
public interface ILimitadorIntentosLogin {

    /** Lanza {@link UsuarioExceptions.DemasiadosIntentosException} si el email o el origen superaron el límite. */
    void verificar(String email, String origen);

    void registrarFallo(String email, String origen);

    /** Un login correcto limpia los fallos del email (no los del origen, para que un atacante no los reinicie). */
    void registrarExito(String email);
}
