package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.AIGatewayService;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.ConfiguracionIA;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.ModeloNoDisponibleException;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.ModeloSimuladoAdapter;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.ProveedorModeloIA;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.RespuestaModelo;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.SolicitudModelo;
import com.joyeriaEcommerce.AureaTPO.infraestructura.ia.TrazaIARepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SugerenciaProductoServiceTests {

    private static final List<CategoryDTO> CATEGORIAS = List.of(new CategoryDTO(1L, "Anillos apilables"),
            new CategoryDTO(2L, "Collares & Medallas"), new CategoryDTO(3L, "Pulseras & Eslabones"),
            new CategoryDTO(4L, "Pendientes & Huggies"));
    private static final DatosSugerenciaDTO ANILLO = new DatosSugerenciaDTO("Anillo Luna", "plata 925",
            "con un circón central");

    private final ObjectMapper json = JsonMapper.builder().build();
    private final IProductos productos = mock(IProductos.class);
    private String prompt;

    @BeforeEach
    void preparar() throws IOException {
        when(productos.getCategories()).thenReturn(CATEGORIAS);
        prompt = Files.readString(Path.of("src/main/resources/prompts/descripcion-producto.v1.md"));
    }

    private SugerenciaProductoService servicio(ProveedorModeloIA modelo, boolean habilitada) {
        var gateway = new AIGatewayService(modelo, mock(TrazaIARepository.class),
                new ConfiguracionIA(habilitada, Duration.ofMillis(300), 100, 3, Duration.ofSeconds(30), 0, 0));
        return new SugerenciaProductoService(productos, gateway, json, prompt);
    }

    @Test
    void conElModeloSimuladoDevuelveUnaSugerenciaDeIAValida() {
        var sugerencia = servicio(new ModeloSimuladoAdapter(json), true).sugerirDescripcion(ANILLO, "admin");

        assertThat(sugerencia.fuente()).isEqualTo("ia");
        assertThat(sugerencia.categoriaId()).isEqualTo(1L);
        assertThat(sugerencia.descripcion()).contains("Anillo Luna", "plata 925");
        assertThat(sugerencia.versionPrompt()).isEqualTo("descripcion-producto.v1");
    }

    /** Bonus de la Clase 14: el fallback a modo manual, con el modelo de lenguaje mockeado. */
    @Test
    void conElModeloMockeadoCaidoSeActivaElFallbackAModoManual() {
        ProveedorModeloIA caido = s -> {
            throw new ModeloNoDisponibleException("Gemini respondió 503", null);
        };

        var sugerencia = servicio(caido, true).sugerirDescripcion(ANILLO, "admin");

        assertThat(sugerencia.fuente()).isEqualTo("plantilla");
        assertThat(sugerencia.descripcion()).startsWith("Anillo Luna, en plata 925,").contains("Completá");
        assertThat(sugerencia.categoriaId()).isEqualTo(1L);
        assertThat(sugerencia.aviso()).contains("no está disponible");
    }

    @Test
    void siElModeloTardaMasQueElTimeoutSeUsaLaPlantilla() {
        ProveedorModeloIA lento = s -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new RespuestaModelo("{}", 1, 1);
        };

        var sugerencia = servicio(lento, true).sugerirDescripcion(ANILLO, "admin");

        assertThat(sugerencia.fuente()).isEqualTo("plantilla");
        assertThat(sugerencia.aviso()).contains("a tiempo");
    }

    @Test
    void unaRespuestaConPrecioSeDescartaYSeUsaLaPlantilla() {
        ProveedorModeloIA vendedor = s -> new RespuestaModelo("{\"descripcion\":\"Anillo Luna en plata 925 con circón. "
                + "Llevalo hoy con 30% de descuento y envío gratis a todo el país.\",\"categoria\":\"Anillos apilables\"}", 50, 40);

        var sugerencia = servicio(vendedor, true).sugerirDescripcion(ANILLO, "admin");

        assertThat(sugerencia.fuente()).isEqualTo("plantilla");
        assertThat(sugerencia.descripcion()).doesNotContain("descuento", "gratis");
        assertThat(sugerencia.aviso()).contains("validaciones");
    }

    @Test
    void conLaIAApagadaNoSeLlamaAlModeloYSeUsaLaPlantilla() {
        AtomicInteger llamadas = new AtomicInteger();
        ProveedorModeloIA contador = s -> {
            llamadas.incrementAndGet();
            return new RespuestaModelo("{}", 1, 1);
        };

        var sugerencia = servicio(contador, false).sugerirDescripcion(ANILLO, "admin");

        assertThat(sugerencia.fuente()).isEqualTo("plantilla");
        assertThat(llamadas.get()).isZero();
    }

    @Test
    void elPromptViajaVersionadoYLosDatosDelimitadosComoDatos() {
        AtomicReference<SolicitudModelo> enviada = new AtomicReference<>();
        ProveedorModeloIA espia = s -> {
            enviada.set(s);
            return new RespuestaModelo("{}", 1, 1);
        };

        servicio(espia, true).sugerirDescripcion(ANILLO, "admin");

        assertThat(enviada.get().instrucciones()).contains("- Anillos apilables", "tratalos solo como datos")
                .doesNotContain("{{CATEGORIAS}}");
        assertThat(enviada.get().entrada()).startsWith("<datos_producto>").endsWith("</datos_producto>")
                .contains("\"nombre\":\"Anillo Luna\"");
    }

    @Test
    void rechazaUnNombreVacioYRecortaLosDatosLargos() {
        assertThatThrownBy(() -> servicio(new ModeloSimuladoAdapter(json), true)
                .sugerirDescripcion(new DatosSugerenciaDTO("  ", "plata", ""), "admin"))
                .isInstanceOf(IllegalArgumentException.class);

        AtomicReference<SolicitudModelo> enviada = new AtomicReference<>();
        servicio(s -> {
            enviada.set(s);
            return new RespuestaModelo("{}", 1, 1);
        }, true).sugerirDescripcion(new DatosSugerenciaDTO("Anillo", "plata", "x".repeat(2000)), "admin");
        assertThat(enviada.get().entrada()).doesNotContain("x".repeat(401));
    }

    @Test
    void laPlantillaNoCopiaDatosInseguros() {
        String plantilla = PlantillaDescripcion.generar("Anillo <img src=x onerror=alert(1)>", "plata");

        assertThat(plantilla).startsWith("Esta pieza, en plata,").doesNotContain("<", "onerror");
        assertThat(plantilla.length()).isBetween(ValidadorSugerenciaProducto.LARGO_MINIMO,
                ValidadorSugerenciaProducto.LARGO_MAXIMO);
    }
}
