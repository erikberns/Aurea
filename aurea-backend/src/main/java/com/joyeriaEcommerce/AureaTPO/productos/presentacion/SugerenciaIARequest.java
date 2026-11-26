package com.joyeriaEcommerce.AureaTPO.productos.presentacion;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.DatosSugerenciaDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de sugerencia: los largos máximos acotan lo que se envía al modelo. */
public record SugerenciaIARequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 80) String material,
        @Size(max = 400) String detalles) {

    public DatosSugerenciaDTO toDatos() {
        return new DatosSugerenciaDTO(nombre, material, detalles);
    }
}
