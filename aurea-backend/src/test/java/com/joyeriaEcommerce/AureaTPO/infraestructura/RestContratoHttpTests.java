package com.joyeriaEcommerce.AureaTPO.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import com.joyeriaEcommerce.AureaTPO.infraestructura.presentacion.IdempotencyFilter;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.IPedidos;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica los requisitos transversales de contrato REST y resiliencia:
 * 1) Errores en formato Problem Details (RFC 9457, application/problem+json).
 * 2) ETag con If-None-Match (304 Not Modified) e If-Match (412 Precondition Failed).
 * 3) Idempotency-Key en POST /api/ordenes (replay sin duplicar y 409 ante payload distinto).
 * 4) DLQ configurada para la cola de órdenes confirmadas.
 */
@SpringBootTest
class RestContratoHttpTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IProductos productos;

    @Autowired
    private IPedidos pedidos;

    @Autowired
    private IJwtService jwtService;

    @Autowired
    private IdempotencyFilter idempotencyFilter;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(idempotencyFilter)
                .build();
    }

    @Test
    void erroresRespondenEnFormatoProblemDetailsRfc9457() throws Exception {
        mvc.perform(get("/api/productos/999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://aurea.local/problemas/solicitud-invalida"))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Producto no encontrado"))
                .andExpect(jsonPath("$.instance").value("/api/productos/999999999"));
    }

    @Test
    void etagSoportaCacheCondicional304YConcurrenciaOptimista412() throws Exception {
        long prodId = productos.createProduct("Collar ETag", "Plata 925", 50000.0, 10, null, "").id();
        String adminJwt = "Bearer " + jwtService.generateToken("admin@aurea.com", "ROLE_ADMIN");

        MvcResult getResult = mvc.perform(get("/api/productos/" + prodId))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();
        String etagInicial = getResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etagInicial).isNotBlank();

        // GET condicional con If-None-Match vigente -> 304 Not Modified sin cuerpo
        mvc.perform(get("/api/productos/" + prodId)
                        .header(HttpHeaders.IF_NONE_MATCH, etagInicial))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, etagInicial));

        // PATCH con If-Match incorrecto -> 412 Precondition Failed en formato Problem Details
        mvc.perform(patch("/api/productos/" + prodId + "/precio")
                        .param("newPrice", "55000")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt)
                        .header(HttpHeaders.IF_MATCH, "\"etag-viejo\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(412))
                .andExpect(jsonPath("$.type").value("https://aurea.local/problemas/precondicion-fallida"));

        // PATCH con If-Match correcto -> 200 OK y nuevo ETag distinto
        MvcResult patchResult = mvc.perform(patch("/api/productos/" + prodId + "/precio")
                        .param("newPrice", "55000")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt)
                        .header(HttpHeaders.IF_MATCH, etagInicial))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();
        String etagNuevo = patchResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etagNuevo).isNotEqualTo(etagInicial);
    }

    @Test
    void idempotencyKeyEvitaDuplicarOrdenYRechazaConflictoDePayload() throws Exception {
        long prodId = productos.createProduct("Anillo Idempotente", "Oro 18k", 30000.0, 20, null, "").id();
        String adminJwt = "Bearer " + jwtService.generateToken("admin@aurea.com", "ROLE_ADMIN");
        int ordenesAntes = pedidos.getOrdersByUser("admin@aurea.com").size();

        String idemKey = UUID.randomUUID().toString();
        String body = "{\"shippingAddress\":\"Av. Corrientes 1234\",\"items\":[{\"productId\":" + prodId + ",\"quantity\":1}]}";

        String primera = mvc.perform(post("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt)
                        .header(IdempotencyFilter.HEADER, idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number orderId = JsonPath.read(primera, "$.id");

        // Reintento idéntico con la misma Idempotency-Key -> misma orden, sin duplicar
        String segunda = mvc.perform(post("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt)
                        .header(IdempotencyFilter.HEADER, idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyFilter.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();
        assertThat((Number) JsonPath.read(segunda, "$.id")).isEqualTo(orderId);
        assertThat(pedidos.getOrdersByUser("admin@aurea.com")).hasSize(ordenesAntes + 1);

        // Misma Idempotency-Key con payload distinto -> 409 Conflict (Problem Details)
        String bodyDistinto = "{\"shippingAddress\":\"Otra Direccion 999\",\"items\":[{\"productId\":" + prodId + ",\"quantity\":2}]}";
        mvc.perform(post("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, adminJwt)
                        .header(IdempotencyFilter.HEADER, idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyDistinto))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://aurea.local/problemas/idempotency-key-conflicto"));
    }

    @Test
    void colaDeOrdenesDeclaraDeadLetterExchangeYColaDlq() {
        RabbitConfiguration config = new RabbitConfiguration();
        Queue colaOrdenes = config.orderConfirmedQueue();
        Queue dlq = config.orderConfirmedDeadLetterQueue();
        assertThat(colaOrdenes.isDurable()).isTrue();
        assertThat(dlq.isDurable()).isTrue();
        assertThat(dlq.getName()).isEqualTo(RabbitConfiguration.ORDER_CONFIRMED_DLQ);
        assertThat(colaOrdenes.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfiguration.ORDERS_DLX)
                .containsEntry("x-dead-letter-routing-key", RabbitConfiguration.ORDER_CONFIRMED_ROUTING_KEY);
    }
}
