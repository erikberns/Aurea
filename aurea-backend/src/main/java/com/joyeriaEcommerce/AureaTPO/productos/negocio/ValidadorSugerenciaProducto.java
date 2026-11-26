package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import tools.jackson.databind.ObjectMapper;

/**
 * Valida la respuesta del modelo antes de mostrarla (OWASP LLM05 y LLM09). Una respuesta se acepta solo si:
 * es JSON con descripcion y categoria; la descripción mide entre 60 y 255 caracteres; la categoría existe;
 * y el texto no trae precios, HTML, enlaces, datos personales, insultos, restos de instrucciones ni
 * materiales que el administrador no cargó.
 */
public final class ValidadorSugerenciaProducto {

    public static final int LARGO_MINIMO = 60;
    public static final int LARGO_MAXIMO = 255;

    /** Materiales y piedras que el modelo no puede agregar si no vienen en los datos. */
    static final List<String> MATERIALES = List.of("oro", "plata", "platino", "acero", "cobre", "bronce", "titanio",
            "vermeil", "rodio", "perla", "diamante", "esmeralda", "rubi", "zafiro", "circon", "cuarzo", "amatista",
            "topacio", "onix", "nacar", "turquesa", "granate", "jade");

    private static final Pattern PRECIO = Pattern.compile(
            "\\$\\s?\\d|\\b\\d[\\d.,]*\\s?(ars|pesos?|usd|dolares?)\\b|\\b(precio|descuento|oferta|gratis|cuotas?|envio|promocion)\\b");
    private static final Pattern HTML_O_ENLACE = Pattern.compile("[<>]|https?://|www\\.|\\]\\(");
    private static final Pattern RESTO_DE_INSTRUCCIONES = Pattern.compile(
            "\\b(ignor\\w*|instruccion\\w*|prompt|jailbreak|modelo de lenguaje|inteligencia artificial)\\b");
    private static final Pattern DATOS_PERSONALES = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+|\\+?\\d[\\d\\s-]{7,}\\d");
    private static final Pattern INSULTOS = Pattern.compile("\\b(idiota|estupid\\w*|imbecil\\w*|basura|inutil\\w*)\\b");

    public record SugerenciaValida(String descripcion, String categoria) {
    }

    private final List<String> categorias;
    private final String datosNormalizados;

    /**
     * @param categorias nombres de las categorías existentes
     * @param datosDelAdministrador todo lo que cargó el administrador (nombre, material y detalles)
     */
    public ValidadorSugerenciaProducto(List<String> categorias, String datosDelAdministrador) {
        this.categorias = List.copyOf(categorias);
        this.datosNormalizados = normalizar(datosDelAdministrador);
    }

    public Optional<SugerenciaValida> validar(String respuesta, ObjectMapper json) {
        if (respuesta == null || respuesta.isBlank()) return Optional.empty();
        Object leido;
        try {
            leido = json.readValue(quitarCercos(respuesta.trim()), Map.class);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        if (!(leido instanceof Map<?, ?> mapa)) return Optional.empty();
        if (!(mapa.get("descripcion") instanceof String descripcion) || !(mapa.get("categoria") instanceof String categoria)) {
            return Optional.empty();
        }
        String limpia = descripcion.strip().replaceAll("\\s+", " ");
        if (limpia.length() < LARGO_MINIMO || limpia.length() > LARGO_MAXIMO || !esSegura(limpia)
                || inventaMateriales(limpia)) {
            return Optional.empty();
        }
        return categorias.stream()
                .filter(c -> normalizar(c).equals(normalizar(categoria.strip())))
                .findFirst()
                .map(c -> new SugerenciaValida(limpia, c));
    }

    /** Reglas de contenido que debe cumplir cualquier texto que se le muestre al administrador. */
    public static boolean esSegura(String texto) {
        String normal = normalizar(texto);
        return !PRECIO.matcher(normal).find()
                && !HTML_O_ENLACE.matcher(texto).find()
                && !RESTO_DE_INSTRUCCIONES.matcher(normal).find()
                && !DATOS_PERSONALES.matcher(normal).find()
                && !INSULTOS.matcher(normal).find();
    }

    private boolean inventaMateriales(String descripcion) {
        String normal = normalizar(descripcion);
        for (String material : MATERIALES) {
            Pattern palabra = Pattern.compile("\\b" + material + "\\w*");
            if (palabra.matcher(normal).find() && !palabra.matcher(datosNormalizados).find()) {
                return true;
            }
        }
        return false;
    }

    /** Minúsculas y sin tildes, para comparar sin depender de la escritura. */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Algunos modelos envuelven el JSON en un bloque ```json ... ```. */
    private static String quitarCercos(String texto) {
        if (!texto.startsWith("```")) return texto;
        int inicio = texto.indexOf('\n');
        int fin = texto.lastIndexOf("```");
        return inicio > 0 && fin > inicio ? texto.substring(inicio + 1, fin).trim() : texto;
    }
}
