package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

public final class UsuarioExceptions {

    private UsuarioExceptions() {
    }

    public static class EmailYaRegistradoException extends RuntimeException {

        public EmailYaRegistradoException(String email) {
            super("Ya existe un usuario registrado con el email " + email);
        }
    }

    public static class UsuarioNoEncontradoException extends RuntimeException {

        public UsuarioNoEncontradoException() {
            super("El usuario solicitado no existe");
        }
    }

    /** Se superó el límite de intentos fallidos de inicio de sesión (HTTP 429). */
    public static class DemasiadosIntentosException extends RuntimeException {

        private final long segundosParaReintentar;

        public DemasiadosIntentosException(long segundosParaReintentar) {
            super("Demasiados intentos fallidos de inicio de sesión. Espere " + segundosParaReintentar
                    + " segundos antes de volver a intentar.");
            this.segundosParaReintentar = segundosParaReintentar;
        }

        public long getSegundosParaReintentar() {
            return segundosParaReintentar;
        }
    }

    public static class CredencialesInvalidasException extends RuntimeException {

        public CredencialesInvalidasException() {
            super("El email o la contraseña son incorrectos");
        }
    }
}
