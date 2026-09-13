package com.joyeriaEcommerce.AureaTPO.carrito;

import com.joyeriaEcommerce.AureaTPO.carrito.negocio.ServicioDeCarrito;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;

@SpringBootTest
class CarritoHttpTests {
 @Autowired WebApplicationContext context;
 @Autowired IProductos productos;
 @Autowired OrderRepository ordenes;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 org.springframework.test.web.servlet.request.RequestPostProcessor token() throws Exception {
  String json=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String valor=JsonPath.read(json,"$.token"),cabecera=JsonPath.read(json,"$.headerName");
  return request -> {
   request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN",valor));
   request.addHeader(cabecera,valor); return request;
  };
 }
 long producto(){return productos.createProduct("Joya prueba", "Prueba", 10000.0, 5, null, "").id();}
 MockHttpSession sesion() throws Exception {
  return (MockHttpSession)mvc.perform(get("/api/carrito")).andExpect(status().isOk()).andReturn().getRequest().getSession();
 }
 void agregar(MockHttpSession s,long id,int cantidad) throws Exception {
  mvc.perform(post("/api/carrito/items").session(s).with(token()).contentType("application/json")
   .content("{\"productoId\":"+id+",\"cantidad\":"+cantidad+"}")).andExpect(status().isOk());
 }
 void registrar(MockHttpSession s) throws Exception {
  mvc.perform(post("/api/usuarios").session(s).with(token()).contentType("application/json")
   .content("{\"nombre\":\"Ana\",\"apellido\":\"Prueba\",\"email\":\""+UUID.randomUUID()+"@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
   .andExpect(status().isCreated());
 }
 @Test void conservaEstadoEntrePeticionesYAislaSesiones() throws Exception {
  long id=producto(); MockHttpSession a=sesion(),b=sesion(); agregar(a,id,2);
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(2));
  mvc.perform(get("/api/carrito").session(b)).andExpect(jsonPath("$.cantidadTotal").value(0));
  assertThat(a.getAttribute("scopedTarget.servicioDeCarrito")).isNotSameAs(b.getAttribute("scopedTarget.servicioDeCarrito"));
  assertThat(context.getBean(IProductos.class)).isSameAs(context.getBean(IProductos.class));
 }
 @Test void logoutDestruyeEstadoYCancelaAutenticacion() throws Exception {
  MockHttpSession a=sesion(); registrar(a); agregar(a,producto(),1);
  ServicioDeCarrito instancia=(ServicioDeCarrito)a.getAttribute("scopedTarget.servicioDeCarrito");
  mvc.perform(post("/api/usuarios/salir").session(a).with(token())).andExpect(status().isOk());
  assertThat(a.isInvalid()).isTrue();
  assertThatThrownBy(instancia::consultar).isInstanceOf(IllegalStateException.class);
  mvc.perform(get("/api/carrito")).andExpect(jsonPath("$.cantidadTotal").value(0));
  mvc.perform(get("/api/ordenes/mis-pedidos")).andExpect(status().isUnauthorized());
 }
 @Test void usaTokenCsrfRealYConservaCarritoInvitadoAlAutenticar() throws Exception {
  MockHttpSession a=sesion(); agregar(a,producto(),1); String anterior=a.getId();
  String respuesta=mvc.perform(get("/api/csrf").session(a)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String token=JsonPath.read(respuesta,"$.token");
  mvc.perform(post("/api/usuarios").session(a)
   .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN",token)).header("X-XSRF-TOKEN",token).contentType("application/json")
   .content("{\"nombre\":\"Ana\",\"apellido\":\"Real\",\"email\":\""+UUID.randomUUID()+"@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
   .andExpect(status().isCreated());
  assertThat(a.getId()).isNotEqualTo(anterior);
  mvc.perform(get("/api/usuarios/sesion").session(a)).andExpect(jsonPath("$.nombre").value("Ana"));
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(1));
 }
 @Test void compraAtomicaVaciaCarritoYDescuentaStock() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a); agregar(a,id,2);
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(24500.0))
   .andExpect(jsonPath("$.status").value("CONFIRMED"))
   .andExpect(jsonPath("$.items[0].product.id").value(id))
   .andExpect(jsonPath("$.items[0].price").value(10000.0))
   .andExpect(jsonPath("$.user.nombre").value("Ana"));
  assertThat(productos.getProductById(id).stock()).isEqualTo(3);
  mvc.perform(get("/api/ordenes/mis-pedidos").session(a))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].items[0].product.id").value(id));
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(0));
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}")).andExpect(status().isBadRequest());
 }
 @Test void cambiarDeCuentaDestruyeElCarritoDeLaSesionAnterior() throws Exception {
  MockHttpSession anterior=sesion(); registrar(anterior); agregar(anterior,producto(),1);
  ServicioDeCarrito carritoAnterior=(ServicioDeCarrito)anterior.getAttribute("scopedTarget.servicioDeCarrito");
  String email=UUID.randomUUID()+"@example.com";
  MockHttpSession nueva=(MockHttpSession)mvc.perform(post("/api/usuarios").session(anterior).with(token())
   .contentType("application/json")
   .content("{\"nombre\":\"Otra\",\"apellido\":\"Cuenta\",\"email\":\""+email+"\",\"contrasena\":\"ClaveSegura123\"}"))
   .andExpect(status().isCreated()).andReturn().getRequest().getSession();
  assertThat(anterior.isInvalid()).isTrue();
  assertThatThrownBy(carritoAnterior::consultar).isInstanceOf(IllegalStateException.class);
  mvc.perform(get("/api/usuarios/sesion").session(nueva)).andExpect(jsonPath("$.email").value(email));
  mvc.perform(get("/api/carrito").session(nueva)).andExpect(jsonPath("$.cantidadTotal").value(0));
 }
 @Test void fallaDeStockConservaSeleccionSinPedido() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a); agregar(a,id,2); productos.updateStock(id,1);
  long antes=ordenes.count();
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}")).andExpect(status().isConflict());
  assertThat(ordenes.count()).isEqualTo(antes);
  assertThat(productos.getProductById(id).stock()).isEqualTo(1);
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(2));
 }
 @Test void rechazaCantidadesInvalidasYExigeRolAdministrador() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a);
  mvc.perform(post("/api/carrito/items").session(a).with(token()).contentType("application/json")
   .content("{\"productoId\":"+id+",\"cantidad\":-1}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/ordenes").session(a).with(token()).contentType("application/json")
   .content("{\"shippingAddress\":\"Calle 123\",\"items\":[{\"productId\":"+id+",\"quantity\":-1}]}"))
   .andExpect(status().isBadRequest());
  mvc.perform(patch("/api/productos/"+id+"/precio").session(a).with(token()).param("newPrice","99"))
   .andExpect(status().isForbidden());
  mvc.perform(patch("/api/productos/"+id+"/precio").with(user("admin").roles("ADMIN")).with(token()).param("newPrice","99"))
   .andExpect(status().isOk());
  mvc.perform(patch("/api/productos/"+id+"/precio").with(user("admin").roles("ADMIN")).with(token()).param("newPrice","-1"))
   .andExpect(status().isBadRequest());
 }
 @Test void mutacionSinCsrfSeRechaza() throws Exception {
  mvc.perform(post("/api/carrito/items").contentType("application/json").content("{\"productoId\":1,\"cantidad\":1}"))
   .andExpect(status().isForbidden());
 }

 @Test void falloDelObserverRevierteConfirmacionYStockEntreComponentes() throws Exception {
  long primero=producto(), segundo=producto();
  MockHttpSession sesion=sesion(); registrar(sesion);
  String json=mvc.perform(post("/api/ordenes").session(sesion).with(token()).contentType("application/json")
   .content("{\"shippingAddress\":\"Calle 123\",\"items\":[{\"productId\":"+primero+",\"quantity\":1},{\"productId\":"+segundo+",\"quantity\":2}]}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"))
   .andReturn().getResponse().getContentAsString();
  Number pedido=JsonPath.read(json,"$.id");
  productos.updateStock(segundo,1);
  mvc.perform(post("/api/ordenes/"+pedido+"/confirmar").session(sesion).with(token()))
   .andExpect(status().isConflict());
  assertThat(productos.getProductById(primero).stock()).isEqualTo(5);
  assertThat(productos.getProductById(segundo).stock()).isEqualTo(1);
  mvc.perform(get("/api/ordenes/mis-pedidos").session(sesion))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"));
 }

 @Test void otroUsuarioNoPuedeConfirmarNiListarUnPedidoAjeno() throws Exception {
  long producto=producto(); MockHttpSession propietario=sesion(),otro=sesion();
  registrar(propietario); registrar(otro);
  String json=mvc.perform(post("/api/ordenes").session(propietario).with(token()).contentType("application/json")
   .content("{\"shippingAddress\":\"Calle 123\",\"items\":[{\"productId\":"+producto+",\"quantity\":1}]}"))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  Number pedido=JsonPath.read(json,"$.id");
  mvc.perform(post("/api/ordenes/"+pedido+"/confirmar").session(otro).with(token()))
   .andExpect(status().isForbidden());
  mvc.perform(get("/api/ordenes/mis-pedidos").session(otro))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
 }
}
