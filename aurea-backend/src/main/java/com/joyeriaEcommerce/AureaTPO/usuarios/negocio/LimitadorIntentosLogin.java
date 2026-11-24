package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ventana fija en memoria: cada clave (email u origen) acumula fallos hasta que vence su ventana.
 * Alcanza para una sola instancia del backend; con varias instancias habría que guardarlo en un
 * almacenamiento compartido (registrado como limitación conocida).
 */
@Component
public class LimitadorIntentosLogin implements ILimitadorIntentosLogin {

    private static final int LIMPIEZA_DESDE = 10_000;

    private final int maxFallosPorEmail;
    private final int maxFallosPorOrigen;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Ventana> fallos = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorIntentosLogin(
            @Value("${app.seguridad.login.max-fallos-email:5}") int maxFallosPorEmail,
            @Value("${app.seguridad.login.max-fallos-origen:20}") int maxFallosPorOrigen,
            @Value("${app.seguridad.login.ventana-segundos:300}") long ventanaSegundos) {
        this(maxFallosPorEmail, maxFallosPorOrigen, Duration.ofSeconds(ventanaSegundos), Clock.systemUTC());
    }

    LimitadorIntentosLogin(int maxFallosPorEmail, int maxFallosPorOrigen, Duration ventana, Clock reloj) {
        if (maxFallosPorEmail < 1 || maxFallosPorOrigen < 1 || ventana.isNegative() || ventana.isZero()) {
            throw new IllegalArgumentException("Configuración inválida del límite de intentos de login");
        }
        this.maxFallosPorEmail = maxFallosPorEmail;
        this.maxFallosPorOrigen = maxFallosPorOrigen;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    @Override
    public void verificar(String email, String origen) {
        Instant ahora = reloj.instant();
        long espera = Math.max(
                segundosDeBloqueo(claveEmail(email), maxFallosPorEmail, ahora),
                segundosDeBloqueo(claveOrigen(origen), maxFallosPorOrigen, ahora));
        if (espera > 0) {
            throw new DemasiadosIntentosException(espera);
        }
    }

    @Override
    public void registrarFallo(String email, String origen) {
        Instant ahora = reloj.instant();
        sumarFallo(claveEmail(email), ahora);
        sumarFallo(claveOrigen(origen), ahora);
        if (fallos.size() > LIMPIEZA_DESDE) {
            fallos.values().removeIf(v -> v.vencida(ahora));
        }
    }

    @Override
    public void registrarExito(String email) {
        fallos.remove(claveEmail(email));
    }

    private long segundosDeBloqueo(String clave, int maximo, Instant ahora) {
        Ventana actual = fallos.get(clave);
        if (actual == null || actual.vencida(ahora) || actual.cantidad() < maximo) {
            return 0;
        }
        // Redondeo hacia arriba: Retry-After nunca indica menos espera de la real.
        return Math.max(1, (Duration.between(ahora, actual.vence()).toMillis() + 999) / 1000);
    }

    private void sumarFallo(String clave, Instant ahora) {
        fallos.compute(clave, (k, actual) -> actual == null || actual.vencida(ahora)
                ? new Ventana(1, ahora.plus(ventana))
                : new Ventana(actual.cantidad() + 1, actual.vence()));
    }

    private static String claveEmail(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String claveOrigen(String origen) {
        return "origen:" + (origen == null ? "desconocido" : origen);
    }

    private record Ventana(int cantidad, Instant vence) {
        boolean vencida(Instant ahora) {
            return !ahora.isBefore(vence);
        }
    }
}
