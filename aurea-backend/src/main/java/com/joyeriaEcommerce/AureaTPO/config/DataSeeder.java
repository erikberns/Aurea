package com.joyeriaEcommerce.AureaTPO.config;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner loadData(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                System.out.println("Cargando joyas de prueba en la base de datos...");

                repository.save(new Product(true, "Plata de Ley 925 con circonitas engastadas a mano.", "Anillo Apilable 'Luna Nueva'", 45000.0, null, 100, null, null));
                repository.save(new Product(true, "Baño de Oro 18k con cadena ajustable (40–45 cm).", "Collar Medalla 'Astro Solar'", 89000.0, null, 50, null, null));
                repository.save(new Product(true, "Plata de Ley 925 rodiada antidesgaste de brillo espejo.", "Pulsera Eslabones 'Aura Link'", 65000.0, null, 75, null, null));
                repository.save(new Product(true, "Plata 925 con baño de Oro 18k y cierre click seguro.", "Pendientes Aros 'Demi Huggies'", 39000.0, null, 200, null, null));
                repository.save(new Product(true, "Oro Vermeil 18k con gema central de topacio blanco natural.", "Anillo Solitario 'Eternity Sparkle'", 120000.0, null, 20, null, null));
                repository.save(new Product(true, "Perlas cultivadas de agua dulce y broche marinero de plata dorada.", "Gargantilla Perlas 'Riviera'", 75000.0, null, 30, null, null));

                System.out.println("Joyas de prueba cargadas exitosamente.");
            }
        };
    }
}
