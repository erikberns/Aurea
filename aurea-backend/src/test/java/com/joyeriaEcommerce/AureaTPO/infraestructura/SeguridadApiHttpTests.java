package com.joyeriaEcommerce.AureaTPO.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de seguridad de la API (OWASP API Top 10 2023 y OWASP Top 10 2021):
 * tokens vencidos o firmados con otra clave, asignación masiva, acceso a datos ajenos,
 * inyección SQL, XSS almacenado y errores que no filtran detalles internos.
 */
@SpringBootTest
class SeguridadApiHttpTests {

    @Autowired WebApplicationContext context;
    @Autowired IJwtService jwt;
    @Value("${app.jwt.secret}") String secretoDelServidor;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Token CSRF real (GET /api/csrf + cookie + cabecera), igual que el frontend. */
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

    private record Cliente(long id, String email, String bearer) {}

    private Cliente registrar() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        MvcResult r = mvc.perform(post("/api/usuarios").with(csrf()).contentType("application/json")
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Prueba\",\"email\":\"" + email
                                + "\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        return new Cliente(id, email, r.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    }

    private String admin() {
        return "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
    }

    // ---------- Autenticación (OWASP API2: Broken Authentication) ----------

    @Test
    void tokenVencidoSeRechazaCon401() throws Exception {
        Cliente cliente = registrar();
        String vencido = new JwtService(secretoDelServidor, -60).generateToken(cliente.email(), "ROLE_CLIENTE");
        mvc.perform(get("/api/ordenes/mis-pedidos").header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ordenes/mis-pedidos").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechazaAunqueDigaSerAdministrador() throws Exception {
        JwtService atacante = new JwtService("clave-del-atacante-distinta-a-la-del-servidor-123456", 900);
        String falso = atacante.generateToken("admin@aurea.com", "ROLE_ADMIN");
        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDeUnUsuarioInexistenteSeRechaza() throws Exception {
        String huerfano = jwt.generateToken("no-existe-" + UUID.randomUUID() + "@example.com", "ROLE_ADMIN");
        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + huerfano))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elRolSaleDeLaBaseYNoDelToken() throws Exception {
        Cliente cliente = registrar();
        // Token bien firmado pero con un rol inventado: los permisos se toman del usuario guardado.
        String inflado = jwt.generateToken(cliente.email(), "ROLE_ADMIN");
        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + inflado))
                .andExpect(status().isForbidden());
    }

    // ---------- Autorización (OWASP API1 BOLA, API3 BOPLA, API5 BFLA) ----------

    @Test
    void registrarseConCamposDeMasNoOtorgaRolDeAdministrador() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        MvcResult r = mvc.perform(post("/api/usuarios").with(csrf()).contentType("application/json")
                        .content("{\"nombre\":\"Eva\",\"apellido\":\"Prueba\",\"email\":\"" + email
                                + "\",\"contrasena\":\"ClaveSegura123\",\"rol\":\"ADMIN\",\"id\":1,"
                                + "\"authorities\":[\"ROLE_ADMIN\"]}"))
                .andReturn();
        int estado = r.getResponse().getStatus();
        // Se acepta ignorar los campos desconocidos (201) o rechazarlos (400); nunca crear un administrador.
        assertThat(estado).isIn(201, 400);
        if (estado == 201) {
            String cuerpo = r.getResponse().getContentAsString();
            assertThat((String) JsonPath.read(cuerpo, "$.rol")).doesNotContainIgnoringCase("ADMIN");
            assertThat(((Number) JsonPath.read(cuerpo, "$.id")).longValue()).isNotEqualTo(1L);
            mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION,
                            r.getResponse().getHeader(HttpHeaders.AUTHORIZATION)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void editarElPerfilNoPermiteCambiarRolNiEmail() throws Exception {
        Cliente cliente = registrar();
        mvc.perform(patch("/api/usuarios/" + cliente.id()).header(HttpHeaders.AUTHORIZATION, cliente.bearer())
                        .contentType("application/json")
                        .content("{\"nombre\":\"Nuevo\",\"apellido\":\"Nombre\",\"rol\":\"ADMIN\","
                                + "\"email\":\"otro@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.email").value(cliente.email()))
                .andExpect(jsonPath("$.rol").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("ADMIN"))));
    }

    @Test
    void unClienteNoPuedeLeerNiEditarElPerfilDeOtro() throws Exception {
        Cliente ana = registrar();
        Cliente beto = registrar();
        mvc.perform(get("/api/usuarios/" + beto.id()).header(HttpHeaders.AUTHORIZATION, ana.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/" + beto.id()).header(HttpHeaders.AUTHORIZATION, ana.bearer())
                        .contentType("application/json").content("{\"nombre\":\"X\",\"apellido\":\"Y\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/" + ana.id() + "/rol").header(HttpHeaders.AUTHORIZATION, ana.bearer())
                        .contentType("application/json").content("{\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + ana.id()).header(HttpHeaders.AUTHORIZATION, ana.bearer()))
                .andExpect(status().isOk());
    }

    // ---------- Inyección (OWASP A03) ----------

    @Test
    void inyeccionSqlEnElLoginNoAutentica() throws Exception {
        mvc.perform(post("/api/usuarios/autenticar").with(csrf())
                        .with(request -> { request.setRemoteAddr("203.0.113.50"); return request; })
                        .contentType("application/json")
                        .content("{\"email\":\"admin@aurea.com\",\"contrasena\":\"' OR '1'='1' --\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios/autenticar").with(csrf())
                        .with(request -> { request.setRemoteAddr("203.0.113.51"); return request; })
                        .contentType("application/json")
                        .content("{\"email\":\"' OR 1=1 --\",\"contrasena\":\"cualquiera\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void textoConSqlSeGuardaLiteralYNoAlteraLaBase() throws Exception {
        String nombre = "Anillo'); DROP TABLE product; --";
        String json = mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType("application/json")
                        .content("{\"name\":\"" + nombre + "\",\"description\":\"x' OR '1'='1\","
                                + "\"price\":100.0,\"stock\":1,\"imageUrl\":\"\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        mvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(nombre));
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
    }

    @Test
    void identificadorConSqlEnLaRutaDevuelve400() throws Exception {
        mvc.perform(get("/api/productos/1 OR 1=1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/productos/abc' OR '1'='1")).andExpect(status().isBadRequest());
    }

    // ---------- XSS y cabeceras (OWASP A03 / A05) ----------

    @Test
    void contenidoConScriptSeDevuelveComoJsonConNosniff() throws Exception {
        String script = "<script>alert('xss')</script>";
        String json = mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType("application/json")
                        .content("{\"name\":\"" + script + "\",\"description\":\"<img src=x onerror=alert(1)>\","
                                + "\"price\":100.0,\"stock\":1,\"imageUrl\":\"\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        // La API devuelve datos, no HTML: JSON + nosniff impiden que el navegador lo interprete como página.
        // El frontend (React) escapa el texto al mostrarlo y no usa dangerouslySetInnerHTML.
        mvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.name").value(script));
    }

    @Test
    void elCatalogoPublicoRespondeAHeadIgualQueAGet() throws Exception {
        mvc.perform(head("/api/productos")).andExpect(status().isOk());
        mvc.perform(head("/api/ordenes")).andExpect(status().isUnauthorized());
    }

    @Test
    void lasRespuestasIncluyenCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Cache-Control"));
    }

    // ---------- Manejo de errores (OWASP A05: no filtrar detalles internos) ----------

    @Test
    void unJsonMalformadoDevuelve400SinDetallesInternos() throws Exception {
        String cuerpo = mvc.perform(post("/api/usuarios").with(csrf()).contentType("application/json")
                        .content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("Exception").doesNotContain("at com.").doesNotContain("jackson");
    }

    @Test
    void unErrorDeNegocioNoMuestraStackTrace() throws Exception {
        String cuerpo = mvc.perform(get("/api/productos/987654321"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("Exception").doesNotContain("at com.").doesNotContain("SQL");
    }
}
