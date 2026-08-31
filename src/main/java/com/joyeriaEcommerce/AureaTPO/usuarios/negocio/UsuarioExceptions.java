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

    public static class CredencialesInvalidasException extends RuntimeException {

        public CredencialesInvalidasException() {
            super("El email o la contrasena son incorrectos");
        }
    }
}
