package com.joyeriaEcommerce.AureaTPO.carrito.presentacion;

import com.joyeriaEcommerce.AureaTPO.carrito.negocio.CarritoDTO;
import com.joyeriaEcommerce.AureaTPO.carrito.negocio.ICarrito;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final ICarrito carrito;

    public CarritoController(ICarrito carrito) {
        this.carrito = carrito;
    }

    @GetMapping
    public CarritoDTO consultar() {
        return carrito.consultar();
    }

    @PostMapping("/items")
    public CarritoDTO agregar(@Valid @RequestBody Agregar request) {
        return carrito.agregar(request.productoId(), request.cantidad());
    }

    @DeleteMapping("/items/{id}")
    public CarritoDTO quitar(@PathVariable Long id) {
        return carrito.quitar(id);
    }

    @DeleteMapping
    public CarritoDTO vaciar() {
        return carrito.vaciar();
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public OrderDTO comprar(@Valid @RequestBody Comprar request, Authentication auth) {
        return carrito.comprar(auth.getName(), request.direccion());
    }

    public record Agregar(@NotNull @Positive Long productoId, @NotNull @Positive Integer cantidad) {}
    public record Comprar(@NotBlank String direccion) {}
}
