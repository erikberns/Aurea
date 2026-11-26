package com.joyeriaEcommerce.AureaTPO.productos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Endpoints del asistente de descripciones, con la seguridad real y el modelo simulado. */
@SpringBootTest
class SugerenciaIAHttpTests {

    @Autowired WebApplicationContext context;
    @Autowired IJwtService jwt;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String admin() {
        return "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
    }

    @Test
    void elAdministradorRecibeUnaSugerenciaYQuedaLaTraza() throws Exception {
        mvc.perform(post("/api/productos/sugerencia-ia").header("Authorization", admin())
                        .contentType("application/json")
                        .content("{\"nombre\":\"Collar Constelación\",\"material\":\"plata 925\","
                                + "\"detalles\":\"con medalla grabada a mano\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("ia"))
                .andExpect(jsonPath("$.categoria").value("Collares & Medallas"))
                .andExpect(jsonPath("$.versionPrompt").value("descripcion-producto.v1"));

        mvc.perform(get("/api/ia/trazas").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.proveedor").value("simulado"))
                .andExpect(jsonPath("$.porResultado.OK").isNumber())
                .andExpect(jsonPath("$.ultimas[0].versionPrompt").value("descripcion-producto.v1"));
    }

    @Test
    void soloElAdministradorPuedeUsarElAsistente() throws Exception {
        mvc.perform(post("/api/productos/sugerencia-ia").with(user("cliente").roles("CLIENTE")).with(csrf())
                        .contentType("application/json").content("{\"nombre\":\"Anillo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/productos/sugerencia-ia/estado")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ia/trazas").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void validaQueHayaNombreYQueLosDatosNoSuperenElLargoMaximo() throws Exception {
        mvc.perform(post("/api/productos/sugerencia-ia").header("Authorization", admin())
                        .contentType("application/json").content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos/sugerencia-ia").header("Authorization", admin())
                        .contentType("application/json")
                        .content("{\"nombre\":\"Anillo\",\"detalles\":\"" + "x".repeat(401) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void informaElEstadoDelAsistente() throws Exception {
        mvc.perform(get("/api/productos/sugerencia-ia/estado").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(true))
                .andExpect(jsonPath("$.proveedor").value("simulado"));
    }
}
