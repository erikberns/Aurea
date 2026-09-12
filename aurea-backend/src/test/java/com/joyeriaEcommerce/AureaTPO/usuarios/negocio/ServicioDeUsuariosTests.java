package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.CredencialesInvalidasException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.EmailYaRegistradoException;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Rol;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ServicioDeUsuariosTests {

    @Autowired
    private IUsuarios usuarios;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registraUnClienteConEmailNormalizadoYContrasenaHasheada() {
        UsuarioDTO registrado = usuarios.registrarCliente(
                new DatosRegistro("  Sofia ", " Perez ", " SOFIA@EXAMPLE.COM ", "ClaveSegura123"));

        assertThat(registrado.email()).isEqualTo("sofia@example.com");
        assertThat(registrado.rol()).isEqualTo(Rol.CLIENTE);

        Usuario persistido = repository.findByEmailIgnoreCase("sofia@example.com").orElseThrow();
        assertThat(persistido.getContrasenaHash()).isNotEqualTo("ClaveSegura123");
        assertThat(passwordEncoder.matches("ClaveSegura123", persistido.getContrasenaHash())).isTrue();
    }

    @Test
    void rechazaUnEmailDuplicadoSinDistinguirMayusculas() {
        usuarios.registrarCliente(new DatosRegistro("Sofia", "Perez", "sofia@example.com", "ClaveSegura123"));

        assertThatThrownBy(() -> usuarios.registrarCliente(
                new DatosRegistro("Otra", "Persona", "SOFIA@example.com", "OtraClave123")))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test
    void autenticaConCredencialesCorrectas() {
        usuarios.registrarCliente(new DatosRegistro("Sofia", "Perez", "sofia@example.com", "ClaveSegura123"));

        UsuarioDTO autenticado = usuarios.autenticar(new Credenciales("sofia@example.com", "ClaveSegura123"));

        assertThat(autenticado.email()).isEqualTo("sofia@example.com");
    }

    @Test
    void cambiaContrasenaConCredencialesValidas() {
        UsuarioDTO registrado = usuarios.registrarCliente(
                new DatosRegistro("Sofia", "Perez", "sofia@example.com", "ClaveSegura123"));

        usuarios.cambiarContrasena(
                registrado.id(),
                new DatosCambioContrasena("ClaveSegura123", "NuevaClave123"));

        Usuario persistido = repository.findByEmailIgnoreCase("sofia@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NuevaClave123", persistido.getContrasenaHash())).isTrue();
        assertThat(usuarios.autenticar(new Credenciales("sofia@example.com", "NuevaClave123"))).isNotNull();
    }

    @Test
    void rechazaCambioDeContrasenaConContrasenaActualIncorrecta() {
        UsuarioDTO registrado = usuarios.registrarCliente(
                new DatosRegistro("Sofia", "Perez", "sofia@example.com", "ClaveSegura123"));

        assertThatThrownBy(() -> usuarios.cambiarContrasena(
                registrado.id(),
                new DatosCambioContrasena("NoEsLaClave", "NuevaClave123")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
