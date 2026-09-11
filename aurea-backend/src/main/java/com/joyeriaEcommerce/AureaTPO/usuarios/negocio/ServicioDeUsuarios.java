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
    private final com.joyeriaEcommerce.AureaTPO.config.JwtService jwtService;

    public ServicioDeUsuarios(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, com.joyeriaEcommerce.AureaTPO.config.JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse registrarCliente(DatosRegistro datos) {
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
        String token = jwtService.generateToken(guardado);
        return new AuthResponse(UsuarioDTO.desde(guardado), token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse autenticar(Credenciales credenciales) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(credenciales.email()))
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(credenciales.contrasena(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generateToken(usuario);
        return new AuthResponse(UsuarioDTO.desde(usuario), token);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO consultarPerfil(Long usuarioId) {
        return UsuarioDTO.desde(buscarUsuario(usuarioId));
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarPerfil(Long usuarioId, DatosActualizacionPerfil datos) {
        Usuario usuario = buscarUsuario(usuarioId);
        usuario.actualizarPerfil(datos.nombre().trim(), datos.apellido().trim());
        return UsuarioDTO.desde(usuario);
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNoEncontradoException::new);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
