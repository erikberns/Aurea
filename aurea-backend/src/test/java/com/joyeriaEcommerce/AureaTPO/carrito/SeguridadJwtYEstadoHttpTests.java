package com.joyeriaEcommerce.AureaTPO.carrito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica por HTTP:
 * 1) Que una transición inválida del patrón State en Order devuelve 409 Conflict.
 * 2) Autenticación stateless con JWT (Bearer), rechazo de alg "none"/alterado (401),
 *    revocación en logout y control de acceso por rol (403 vs 200).
 */
@SpringBootTest
class SeguridadJwtYEstadoHttpTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IProductos productos;

    @Autowired
    private IJwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private RequestPostProcessor csrf() throws Exception {
        String json = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String valor = JsonPath.read(json, "$.token");
        String cabecera = JsonPath.read(json, "$.headerName");
        return request -> {
            request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", valor));
            request.addHeader(cabecera, valor);
            return request;
        };
    }

    @Test
    void transicionInvalidaEnOrdenDevuelve409Conflict() throws Exception {
        long prodId = productos.createProduct("Anillo Test State", "Desc", 20000.0, 10, null, "").id();
        String email = UUID.randomUUID() + "@example.com";

        MvcResult regResult = mvc.perform(post("/api/usuarios").with(csrf())
                        .contentType("application/json")
                        .content("{\"nombre\":\"Lucia\",\"apellido\":\"Gomez\",\"email\":\"" + email + "\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String bearer = regResult.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        assertThat(bearer).startsWith("Bearer ");

        // Crear orden en estado PENDING
        String orderJson = mvc.perform(post("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType("application/json")
                        .content("{\"shippingAddress\":\"Av. Libertador 100\",\"items\":[{\"productId\":" + prodId + ",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Number orderId = JsonPath.read(orderJson, "$.id");

        // 1ra confirmación: válida (PENDING -> CONFIRMED) -> 200 OK
        mvc.perform(post("/api/ordenes/" + orderId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 2da confirmación sobre orden ya CONFIRMED: transición inválida -> 409 Conflict
        mvc.perform(post("/api/ordenes/" + orderId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Admin aprueba devolución (CONFIRMED -> CANCELLED) -> 200 OK
        String adminJwt = "Bearer " + jwtService.generateToken("admin@aurea.com", "ROLE_ADMIN");
        mvc.perform(post("/api/ordenes/" + orderId + "/devolucion/aprobar")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Intentar cancelar nuevamente una orden ya CANCELLED -> 409 Conflict
        mvc.perform(post("/api/ordenes/" + orderId + "/devolucion/aprobar")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void autenticacionJwtValidaRechazaTokenAlteradoAlgNoneYRevocaEnLogout() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/usuarios").with(csrf())
                        .contentType("application/json")
                        .content("{\"nombre\":\"Marcos\",\"apellido\":\"Diaz\",\"email\":\"" + email + "\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/usuarios/autenticar").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String bearer = login.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        assertThat(bearer).isNotBlank().startsWith("Bearer ");

        // Acceso autenticado solo con JWT Bearer (sin sesión HTTP)
        mvc.perform(get("/api/ordenes/mis-pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        // Control de acceso por rol: CLIENTE recibe 403 en endpoint ADMIN
        mvc.perform(get("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());

        // Token con alg "none" es rechazado con 401 (SEC-W03)
        String noneHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = bearer.substring(7).split("\\.")[1];
        String algNoneToken = "Bearer " + noneHeader + "." + payload + ".firma";
        mvc.perform(get("/api/ordenes/mis-pedidos")
                        .header(HttpHeaders.AUTHORIZATION, algNoneToken))
                .andExpect(status().isUnauthorized());

        // Token con firma alterada es rechazado con 401 (SEC-W03)
        mvc.perform(get("/api/ordenes/mis-pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer + " alterada"))
                .andExpect(status().isUnauthorized());

        // Logout revoca el token JWT y ya no permite autenticar (SEC-W04)
        mvc.perform(post("/api/usuarios/salir")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        mvc.perform(get("/api/ordenes/mis-pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }
}

