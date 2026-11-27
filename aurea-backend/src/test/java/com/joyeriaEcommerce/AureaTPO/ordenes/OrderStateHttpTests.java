package com.joyeriaEcommerce.AureaTPO.ordenes;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.Order;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Las transiciones inválidas del pedido llegan a la API como 409 Conflict en formato Problem Details. */
@SpringBootTest
class OrderStateHttpTests {

    @Autowired WebApplicationContext context;
    @Autowired OrderRepository ordenes;
    @Autowired IJwtService jwt;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private record Cliente(long id, String bearer) {}

    private Cliente registrar() throws Exception {
        String json = mvc.perform(get("/api/csrf")).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.token");
        String cabecera = JsonPath.read(json, "$.headerName");
        MvcResult r = mvc.perform(post("/api/usuarios")
                        .with(req -> {
                            req.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token));
                            req.addHeader(cabecera, token);
                            return req;
                        })
                        .contentType("application/json")
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Estado\",\"email\":\"" + UUID.randomUUID()
                                + "@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        return new Cliente(id, r.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    }

    private long pedidoEn(OrderStatus estado, long usuarioId) {
        return ordenes.save(new Order("Av. Corrientes 1234", LocalDate.now(), estado, 1000.0, usuarioId)).getId();
    }

    @ParameterizedTest(name = "confirmar un pedido {0} responde 409")
    @EnumSource(value = OrderStatus.class, names = {"CONFIRMED", "COMPLETED", "CANCELLED"})
    void confirmarFueraDePendienteResponde409(OrderStatus estado) throws Exception {
        Cliente cliente = registrar();
        long id = pedidoEn(estado, cliente.id());
        mvc.perform(post("/api/ordenes/" + id + "/confirmar").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(estado.name())));
        org.assertj.core.api.Assertions.assertThat(ordenes.findById(id).orElseThrow().getStatus()).isEqualTo(estado);
    }

    @ParameterizedTest(name = "aprobar la devolución de un pedido {0} responde 409")
    @EnumSource(value = OrderStatus.class, names = {"CANCELLED"})
    void aprobarDevolucionDeUnPedidoCanceladoResponde409(OrderStatus estado) throws Exception {
        Cliente cliente = registrar();
        long id = pedidoEn(estado, cliente.id());
        String admin = "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
        mvc.perform(post("/api/ordenes/" + id + "/devolucion/aprobar").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @ParameterizedTest(name = "aprobar la devolución de un pedido {0} lo cancela")
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "COMPLETED"})
    void aprobarDevolucionEsValidaDesdeLosDemasEstados(OrderStatus estado) throws Exception {
        Cliente cliente = registrar();
        long id = pedidoEn(estado, cliente.id());
        String admin = "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
        mvc.perform(post("/api/ordenes/" + id + "/devolucion/aprobar").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
