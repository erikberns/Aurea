package com.joyeriaEcommerce.AureaTPO.usuarios.presentacion;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.IUsuarios;
import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final IUsuarios usuarios;
    private final AuthenticationManager authenticationManager;

    public UsuarioController(IUsuarios usuarios, AuthenticationManager authenticationManager) {
        this.usuarios = usuarios;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO registrar(@Valid @RequestBody RegistrarClienteRequest requestBody, HttpServletRequest httpRequest) {
        UsuarioDTO user = usuarios.registrarCliente(requestBody.toDatos());
        
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestBody.email(), requestBody.contrasena())
        );
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        
        return user;
    }

    @PostMapping("/autenticar")
    public UsuarioDTO autenticar(@Valid @RequestBody AutenticarRequest requestBody, HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestBody.email(), requestBody.contrasena())
        );
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        return usuarios.autenticar(requestBody.toCredenciales());
    }

    @GetMapping("/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #usuarioId == principal.id")
    public UsuarioDTO consultarPerfil(@PathVariable Long usuarioId) {
        return usuarios.consultarPerfil(usuarioId);
    }

    @PatchMapping("/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #usuarioId == principal.id")
    public UsuarioDTO actualizarPerfil(
            @PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return usuarios.actualizarPerfil(usuarioId, request.toDatos());
    }

    @PatchMapping("/{usuarioId}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #usuarioId == principal.id")
    public void cambiarContrasena(
            @PathVariable Long usuarioId,
            @Valid @RequestBody CambiarContrasenaRequest request) {
        usuarios.cambiarContrasena(usuarioId, request.toDatos());
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public java.util.List<UsuarioDTO> obtenerTodos() {
        return usuarios.obtenerTodos();
    }

    @PatchMapping("/{usuarioId}/rol")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public UsuarioDTO asignarRol(
            @PathVariable Long usuarioId,
            @RequestBody java.util.Map<String, String> request) {
        return usuarios.asignarRol(usuarioId, request.get("rol"));
    }
}
