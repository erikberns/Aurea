package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Rol;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.CredencialesInvalidasException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.EmailYaRegistradoException;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.UsuarioNoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioDeUsuarios implements IUsuarios {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    public ServicioDeUsuarios(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioDTO registrarCliente(DatosRegistro datos) {
        String email = normalizarEmail(datos.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailYaRegistradoException(email);
        }

        Usuario usuario = new Usuario(
                datos.nombre().trim(),
                datos.apellido().trim(),
                email,
                passwordEncoder.encode(datos.contrasena()),
                Rol.CLIENTE);

        Usuario guardado = usuarioRepository.save(usuario);
        return toDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO autenticar(Credenciales credenciales) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(credenciales.email()))
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(credenciales.contrasena(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        return toDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO consultarPerfil(Long usuarioId) {
        return toDTO(buscarUsuario(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO consultarPerfilPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .orElseThrow(UsuarioNoEncontradoException::new);
        return toDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarPerfil(Long usuarioId, DatosActualizacionPerfil datos) {
        Usuario usuario = buscarUsuario(usuarioId);
        usuario.actualizarPerfil(datos.nombre().trim(), datos.apellido().trim());
        return toDTO(usuario);
    }

    @Override
    @Transactional
    public void cambiarContrasena(Long usuarioId, DatosCambioContrasena datos) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (!passwordEncoder.matches(datos.contrasenaActual(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }
        usuario.cambiarContrasena(passwordEncoder.encode(datos.contrasenaNueva()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UsuarioDTO> obtenerTodos() {
        return usuarioRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public UsuarioDTO asignarRol(Long usuarioId, String rol) {
        Usuario usuario = buscarUsuario(usuarioId);
        try {
            Rol nuevoRol = Rol.valueOf(rol.toUpperCase());
            usuario.setRole(nuevoRol);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido");
        }
        return toDTO(usuario);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getFirstName(), usuario.getLastName(),
                usuario.getEmail(), usuario.getRole().name());
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNoEncontradoException::new);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
