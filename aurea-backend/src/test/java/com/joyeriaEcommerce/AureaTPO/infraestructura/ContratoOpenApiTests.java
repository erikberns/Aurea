package com.joyeriaEcommerce.AureaTPO.infraestructura;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** El contrato OpenAPI se publica sin autenticación y describe los recursos principales de la API. */
@SpringBootTest
class ContratoOpenApiTests {

    @Autowired WebApplicationContext context;

    @Test
    void publicaElContratoConLosRecursosPrincipales() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Áurea Joyas — API REST"))
                .andExpect(jsonPath("$.paths['/api/productos']").exists())
                .andExpect(jsonPath("$.paths['/api/productos/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios']").exists())
                .andExpect(jsonPath("$.paths['/api/carrito/checkout']").exists())
                .andExpect(jsonPath("$.paths['/api/ordenes/{id}/confirmar']").exists())
                .andExpect(jsonPath("$.paths['/api/reposiciones']").exists())
                // Los campos opcionales de los DTO admiten null (por ejemplo, un producto sin descripción).
                .andExpect(jsonPath("$.components.schemas.ProductDTO.properties.description")
                        .value(org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString("null"))));
    }
}
