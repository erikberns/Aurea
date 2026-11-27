package com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales del contrato OpenAPI que springdoc publica en GET /v3/api-docs.
 * El detalle de cada endpoint sale de los controladores; las pruebas de contrato (Schemathesis) lo usan como fuente.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI contratoAurea() {
        return new OpenAPI()
                .info(new Info()
                        .title("Áurea Joyas — API REST")
                        .version("1.0")
                        .description("Catálogo, usuarios, bolsa de compras, pedidos, pagos, reposiciones y asistente de IA. "
                                + "Los errores siguen Problem Details (RFC 9457)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /**
     * Los DTO son records de Java: un campo opcional (descripción de un producto, detalle de un pago, categoría)
     * puede viajar como null. springdoc los declara como "string" u objeto a secas; este ajuste agrega "null"
     * a todo campo que no esté marcado como obligatorio, para que el contrato describa lo que la API devuelve.
     */
    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    OpenApiCustomizer camposOpcionalesAdmitenNull() {
        return api -> {
            if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
                return;
            }
            for (Schema esquema : api.getComponents().getSchemas().values()) {
                Map<String, Schema> propiedades = esquema.getProperties();
                if (propiedades == null) {
                    continue;
                }
                List<String> obligatorios = esquema.getRequired() == null ? List.of() : esquema.getRequired();
                propiedades.replaceAll((nombre, propiedad) -> {
                    if (obligatorios.contains(nombre)) {
                        return propiedad;
                    }
                    if (propiedad.get$ref() != null) {
                        return new Schema().anyOf(List.of(propiedad, new Schema().types(Set.of("null"))));
                    }
                    if (propiedad.getTypes() != null && !propiedad.getTypes().isEmpty()) {
                        propiedad.addType("null");
                    } else {
                        propiedad.setNullable(true);
                    }
                    return propiedad;
                });
            }
        };
    }
}
