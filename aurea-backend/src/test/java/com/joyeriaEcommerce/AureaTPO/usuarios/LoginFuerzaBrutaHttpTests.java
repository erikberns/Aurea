package com.joyeriaEcommerce.AureaTPO.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** CP-38: fuerza bruta sobre el login (OWASP API4: consumo de recursos sin límite). */
@SpringBootTest
class LoginFuerzaBrutaHttpTests {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /**
     * Token CSRF real (GET /api/csrf + cookie + cabecera). No se usa csrf() de spring-security-test porque
     * reemplaza el repositorio de tokens del filtro compartido y rompe a las clases que corren después.
     */
    private RequestPostProcessor csrf() throws Exception {
        String json = mvc.perform(get("/api/csrf")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String valor = JsonPath.read(json, "$.token");
        String cabecera = JsonPath.read(json, "$.headerName");
        return request -> {
            request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", valor));
            request.addHeader(cabecera, valor);
            return request;
        };
    }

    private ResultActions login(String email, String contrasena, String ip) throws Exception {
        return mvc.perform(post("/api/usuarios/autenticar").with(csrf())
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    @Test
    void trasCincoFallosElLoginResponde429ConRetryAfterEnProblemDetails() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        for (int i = 0; i < 5; i++) {
            login(email, "ClaveIncorrecta" + i, "198.51.100.7").andExpect(status().isUnauthorized());
        }
        String retryAfter = login(email, "ClaveIncorrecta", "198.51.100.8")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.type").value("https://aurea.local/problemas/demasiados-intentos"))
                .andExpect(jsonPath("$.reintentarEnSegundos").isNumber())
                .andReturn().getResponse().getHeader("Retry-After");
        assertThat(Long.parseLong(retryAfter)).isPositive();
    }

    @Test
    void laContrasenaCorrectaTampocoEntraMientrasDuraElBloqueo() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/usuarios").with(csrf()).contentType("application/json")
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Prueba\",\"email\":\"" + email
                                + "\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) {
            login(email, "ClaveIncorrecta" + i, "198.51.100.20").andExpect(status().isUnauthorized());
        }
        login(email, "ClaveSegura123", "198.51.100.20").andExpect(status().isTooManyRequests());
    }
}
