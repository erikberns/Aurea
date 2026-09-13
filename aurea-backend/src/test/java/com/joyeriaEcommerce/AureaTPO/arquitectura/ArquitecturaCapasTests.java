package com.joyeriaEcommerce.AureaTPO.arquitectura;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.WildcardType;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica las convenciones de paquetes y las dependencias directas entre capas. */
class ArquitecturaCapasTests {
    private static final Path BASE = Path.of("src/main/java/com/joyeriaEcommerce/AureaTPO");
    private static final Set<String> COMPONENTES = Set.of("usuarios", "carrito", "productos", "ordenes");
    private static final Set<String> CAPAS = Set.of("presentacion", "negocio", "datos");
    private static final Pattern REFERENCIA = Pattern.compile(
            "com\\.joyeriaEcommerce\\.AureaTPO\\.(usuarios|carrito|productos|ordenes)\\.(presentacion|negocio|datos)\\.");

    @Test
    void cadaComponenteContieneLasTresCapasYNoTieneClasesFueraDeEllas() throws IOException {
        for (String componente : COMPONENTES) {
            for (String capa : CAPAS) {
                assertThat(fuentes(BASE.resolve(componente).resolve(capa)))
                        .as("%s/%s debe contener implementación", componente, capa).isNotEmpty();
            }
        }
        for (Path archivo : fuentes(BASE)) {
            Path relativo = BASE.relativize(archivo);
            if (relativo.getNameCount() == 1) {
                assertThat(relativo.toString()).isEqualTo("AureaTpoApplication.java");
                continue;
            }
            String raiz = relativo.getName(0).toString();
            if (raiz.equals("infraestructura")) {
                continue; // Configuración y soporte transversal, no un componente de negocio.
            }
            assertThat(COMPONENTES).as("Componente de %s", relativo).contains(raiz);
            assertThat(CAPAS).as("Capa de %s", relativo).contains(relativo.getName(1).toString());
        }
    }

    @Test
    void presentacionNoAccedeADatosYLasCapasInferioresNoDependenDePresentacion() throws IOException {
        for (String componente : COMPONENTES) {
            for (String capa : CAPAS) {
                for (Path archivo : fuentes(BASE.resolve(componente).resolve(capa))) {
                    String codigo = Files.readString(archivo);
                    var referencias = REFERENCIA.matcher(codigo);
                    while (referencias.find()) {
                        String destino = referencias.group(2);
                        if (capa.equals("presentacion")) {
                            assertThat(destino).as("Presentación no accede a datos: %s", archivo)
                                    .isNotEqualTo("datos");
                        } else if (capa.equals("negocio")) {
                            assertThat(destino).as("Negocio no depende de presentación: %s", archivo)
                                    .isNotEqualTo("presentacion");
                        } else {
                            assertThat(destino).as("Datos no depende de capas superiores: %s", archivo)
                                    .isEqualTo("datos");
                        }
                    }
                }
            }
        }
    }

    private static List<Path> fuentes(Path directorio) throws IOException {
        try (var archivos = Files.walk(directorio)) {
            return archivos.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void unComponenteSoloConoceContratosDeNegocioDeOtro() throws Exception {
        Pattern referenciaCompleta = Pattern.compile(
                "com\\.joyeriaEcommerce\\.AureaTPO\\.(usuarios|carrito|productos|ordenes)\\.(presentacion|negocio|datos)\\.([\\w.$]+)");
        for (String origen : COMPONENTES) {
            for (Path archivo : fuentes(BASE.resolve(origen))) {
                String codigo = Files.readString(archivo).replaceAll("(?m)^package .*;", "");
                assertThat(codigo).as("Importar tipos explícitos: %s", archivo)
                        .doesNotContainPattern("import com\\.joyeriaEcommerce\\..*\\.\\*");
                var referencias = referenciaCompleta.matcher(codigo);
                while (referencias.find()) {
                    if (origen.equals(referencias.group(1))) continue;
                    assertThat(referencias.group(2)).as("Acceso a otra capa interna desde %s", archivo)
                            .isEqualTo("negocio");
                    Class<?> tipo = resolver(referencias.group());
                    assertThat(tipo.isInterface() || tipo.isRecord() || tipo.isEnum())
                            .as("%s consume implementación ajena %s", archivo, tipo.getName()).isTrue();
                }
            }
        }
    }

    @Test
    void losBeansRecibenDependenciasDelProyectoPorInterfaz() throws Exception {
        for (String componente : COMPONENTES) {
            for (Path archivo : fuentes(BASE.resolve(componente))) {
                Class<?> tipo = claseDe(archivo);
                boolean bean = tipo.isAnnotationPresent(org.springframework.stereotype.Service.class)
                        || tipo.isAnnotationPresent(org.springframework.stereotype.Component.class)
                        || tipo.isAnnotationPresent(org.springframework.stereotype.Repository.class)
                        || tipo.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class);
                if (!bean) continue;
                for (var constructor : tipo.getDeclaredConstructors()) {
                    for (Class<?> dependencia : constructor.getParameterTypes()) {
                        if (esDelProyecto(dependencia)) {
                            assertThat(dependencia.isInterface())
                                    .as("%s debe inyectar un contrato, no %s", tipo.getSimpleName(), dependencia.getSimpleName())
                                    .isTrue();
                        }
                    }
                }
                assertThat(Files.readString(archivo)).as("El contenedor crea dependencias: %s", archivo)
                        .doesNotContainPattern("new\\s+[\\w.]+(?:Service|Repository|Facade)\\s*\\(");
            }
        }
    }

    @Test
    void interfacesYDtoNoExponenEntidadesNiImplementaciones() throws Exception {
        for (String componente : COMPONENTES) {
            for (Path archivo : fuentes(BASE.resolve(componente).resolve("negocio"))) {
                Class<?> tipo = claseDe(archivo);
                if (tipo.isInterface() || tipo.isRecord()) {
                    verificarContrato(tipo, new HashSet<>());
                }
            }
        }
    }

    private static void verificarContrato(Type tipo, Set<Type> visitados) {
        if (!visitados.add(tipo)) return;
        if (tipo instanceof ParameterizedType parametrizado) {
            verificarContrato(parametrizado.getRawType(), visitados);
            for (Type argumento : parametrizado.getActualTypeArguments()) verificarContrato(argumento, visitados);
        } else if (tipo instanceof GenericArrayType arreglo) {
            verificarContrato(arreglo.getGenericComponentType(), visitados);
        } else if (tipo instanceof WildcardType comodin) {
            for (Type limite : comodin.getUpperBounds()) verificarContrato(limite, visitados);
            for (Type limite : comodin.getLowerBounds()) verificarContrato(limite, visitados);
        } else if (tipo instanceof Class<?> clase) {
            if (clase.isArray()) { verificarContrato(clase.getComponentType(), visitados); return; }
            if (!esDelProyecto(clase)) return;
            assertThat(clase.getName()).doesNotContain(".datos.", ".presentacion.");
            assertThat(clase.isRecord() || clase.isInterface() || clase.isEnum())
                    .as("Tipo expuesto en un contrato: %s", clase.getName()).isTrue();
            if (clase.isRecord()) {
                for (var campo : clase.getRecordComponents()) verificarContrato(campo.getGenericType(), visitados);
            }
            for (var metodo : clase.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(metodo.getModifiers())) continue;
                verificarContrato(metodo.getGenericReturnType(), visitados);
                for (Type parametro : metodo.getGenericParameterTypes()) verificarContrato(parametro, visitados);
            }
        }
    }

    private static boolean esDelProyecto(Class<?> tipo) {
        return tipo.getName().startsWith("com.joyeriaEcommerce.AureaTPO.");
    }

    private static Class<?> claseDe(Path archivo) throws ClassNotFoundException {
        String relativo = BASE.relativize(archivo).toString().replace('\\', '.').replace('/', '.');
        return Class.forName("com.joyeriaEcommerce.AureaTPO." + relativo.replace(".java", ""), false,
                ArquitecturaCapasTests.class.getClassLoader());
    }

    private static Class<?> resolver(String nombre) throws ClassNotFoundException {
        try {
            return Class.forName(nombre, false, ArquitecturaCapasTests.class.getClassLoader());
        } catch (ClassNotFoundException error) {
            int punto = nombre.lastIndexOf('.');
            if (punto < 0) throw error;
            try {
                return Class.forName(nombre.substring(0, punto) + "$" + nombre.substring(punto + 1), false,
                        ArquitecturaCapasTests.class.getClassLoader());
            } catch (ClassNotFoundException anidada) {
                throw error;
            }
        }
    }
}
