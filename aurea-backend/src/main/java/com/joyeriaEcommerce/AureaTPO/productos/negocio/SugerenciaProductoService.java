package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.EstadoGatewayIA;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.IAGateway;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.PedidoIA;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.ResultadoIA;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.SolicitudModelo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Asistente de descripciones: arma el pedido con el prompt versionado, lo manda por el gateway,
 * valida la respuesta y, si no hay una respuesta válida, devuelve la plantilla de fallback.
 * Nada se guarda acá: el administrador revisa la sugerencia y decide (humano en el circuito).
 */
@Service
public class SugerenciaProductoService implements ISugerenciasProducto {

    public static final String FUNCIONALIDAD = "descripcion-producto";
    public static final String VERSION_PROMPT = "descripcion-producto.v1";
    static final int MAX_NOMBRE = 120;
    static final int MAX_MATERIAL = 80;
    static final int MAX_DETALLES = 400;

    private final IProductos productos;
    private final IAGateway ia;
    private final ObjectMapper json;
    private final String prompt;

    @Autowired
    public SugerenciaProductoService(IProductos productos, IAGateway ia, ObjectMapper json,
                                     @Value("classpath:prompts/descripcion-producto.v1.md") Resource prompt)
            throws IOException {
        this(productos, ia, json, prompt.getContentAsString(StandardCharsets.UTF_8));
    }

    public SugerenciaProductoService(IProductos productos, IAGateway ia, ObjectMapper json, String prompt) {
        if (prompt == null || !prompt.contains("{{CATEGORIAS}}")) {
            throw new IllegalStateException("El prompt " + VERSION_PROMPT + " no tiene el marcador {{CATEGORIAS}}");
        }
        this.productos = productos;
        this.ia = ia;
        this.json = json;
        this.prompt = prompt;
    }

    @Override
    public SugerenciaProductoDTO sugerirDescripcion(DatosSugerenciaDTO datos, String usuario) {
        String nombre = limpiar(datos == null ? null : datos.nombre(), MAX_NOMBRE);
        if (nombre.isBlank()) throw new IllegalArgumentException("El nombre de la pieza es obligatorio");
        String material = limpiar(datos.material(), MAX_MATERIAL);
        String detalles = limpiar(datos.detalles(), MAX_DETALLES);

        List<CategoryDTO> categorias = productos.getCategories();
        List<String> nombresCategorias = categorias.stream().map(CategoryDTO::description).toList();

        String instrucciones = prompt.replace("{{CATEGORIAS}}",
                nombresCategorias.stream().map(c -> "- " + c).collect(Collectors.joining("\n")));
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("nombre", nombre);
        entrada.put("material", material);
        entrada.put("detalles", detalles);
        entrada.put("categoriasPermitidas", nombresCategorias);
        String datosDelimitados = "<datos_producto>\n" + json.writeValueAsString(entrada) + "\n</datos_producto>";

        var validador = new ValidadorSugerenciaProducto(nombresCategorias, nombre + " " + material + " " + detalles);
        ResultadoIA<ValidadorSugerenciaProducto.SugerenciaValida> resultado = ia.completar(
                new PedidoIA(FUNCIONALIDAD, VERSION_PROMPT, usuario,
                        new SolicitudModelo(instrucciones, datosDelimitados, 1024, 0.4)),
                texto -> validador.validar(texto, json));

        if (resultado.exito()) {
            var sugerencia = resultado.valor();
            CategoryDTO categoria = categorias.stream()
                    .filter(c -> c.description().equals(sugerencia.categoria()))
                    .findFirst().orElse(null);
            return new SugerenciaProductoDTO(sugerencia.descripcion(),
                    categoria == null ? null : categoria.id(), categoria == null ? null : categoria.description(),
                    "ia", VERSION_PROMPT, "Sugerencia generada con IA: revisala y ajustala antes de guardar.");
        }
        CategoryDTO categoria = PlantillaDescripcion.categoriaPorNombre(nombre, categorias);
        return new SugerenciaProductoDTO(PlantillaDescripcion.generar(nombre, material),
                categoria == null ? null : categoria.id(), categoria == null ? null : categoria.description(),
                "plantilla", VERSION_PROMPT, resultado.aviso());
    }

    @Override
    public EstadoAsistenteIADTO estado() {
        EstadoGatewayIA estado = ia.estado();
        return new EstadoAsistenteIADTO(estado.habilitada(), estado.proveedor(), estado.modelo());
    }

    /** Quita caracteres de control, junta espacios y recorta al largo máximo (OWASP LLM10). */
    private static String limpiar(String valor, int maximo) {
        if (valor == null) return "";
        String limpio = valor.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo).strip();
    }
}
