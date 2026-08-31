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

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final IUsuarios usuarios;

    public UsuarioController(IUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO registrar(@Valid @RequestBody RegistrarClienteRequest request) {
        return usuarios.registrarCliente(request.toDatos());
    }

    @PostMapping("/autenticar")
    public UsuarioDTO autenticar(@Valid @RequestBody AutenticarRequest request) {
        return usuarios.autenticar(request.toCredenciales());
    }

    @GetMapping("/{usuarioId}")
    public UsuarioDTO consultarPerfil(@PathVariable Long usuarioId) {
        return usuarios.consultarPerfil(usuarioId);
    }

    @PatchMapping("/{usuarioId}")
    public UsuarioDTO actualizarPerfil(
            @PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return usuarios.actualizarPerfil(usuarioId, request.toDatos());
    }
}
