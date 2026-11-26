package com.joyeriaEcommerce.AureaTPO.productos.negocio;

/**
 * Sugerencia para la ficha de un producto. Nunca se guarda sola: el administrador la revisa y decide.
 *
 * @param fuente        "ia" si la generó el modelo y pasó las validaciones; "plantilla" si se usó el fallback
 * @param categoriaId   categoría sugerida (puede ser nula en la plantilla)
 * @param versionPrompt versión del prompt con que se pidió
 * @param aviso         mensaje para mostrar junto a la sugerencia
 */
public record SugerenciaProductoDTO(String descripcion, Long categoriaId, String categoria, String fuente,
                                    String versionPrompt, String aviso) {
}
