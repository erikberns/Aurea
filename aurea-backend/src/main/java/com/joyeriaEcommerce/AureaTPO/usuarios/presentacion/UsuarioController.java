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

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.AuthResponse;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final IUsuarios usuarios;

    public UsuarioController(IUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegistrarClienteRequest request) {
        return usuarios.registrarCliente(request.toDatos());
    }

    @PostMapping("/autenticar")
    public AuthResponse autenticar(@Valid @RequestBody AutenticarRequest request) {
        return usuarios.autenticar(request.toCredenciales());
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
