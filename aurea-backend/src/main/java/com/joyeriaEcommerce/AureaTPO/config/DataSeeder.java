package com.joyeriaEcommerce.AureaTPO.config;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductRepository;
import com.joyeriaEcommerce.AureaTPO.categorias.datos.Category;
import com.joyeriaEcommerce.AureaTPO.categorias.datos.CategoryRepository;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Rol;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner loadData(ProductRepository productRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, CategoryRepository categoryRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                System.out.println("Cargando joyas de prueba en la base de datos...");

                Category anillos = categoryRepository.save(new Category("Anillos apilables", null));
                Category collares = categoryRepository.save(new Category("Collares & Medallas", null));
                Category pulseras = categoryRepository.save(new Category("Pulseras & Eslabones", null));
                Category pendientes = categoryRepository.save(new Category("Pendientes & Huggies", null));

                productRepository.save(new Product(true, "Plata de Ley 925 con circonitas engastadas a mano.", "Anillo Apilable 'Luna Nueva'", 45000.0, null, 100, anillos, null));
                productRepository.save(new Product(true, "Baño de Oro 18k con cadena ajustable (40–45 cm).", "Collar Medalla 'Astro Solar'", 89000.0, null, 50, collares, null));
                productRepository.save(new Product(true, "Plata de Ley 925 rodiada antidesgaste de brillo espejo.", "Pulsera Eslabones 'Aura Link'", 65000.0, null, 75, pulseras, null));
                productRepository.save(new Product(true, "Plata 925 con baño de Oro 18k y cierre click seguro.", "Pendientes Aros 'Demi Huggies'", 39000.0, null, 200, pendientes, null));
                productRepository.save(new Product(true, "Oro Vermeil 18k con gema central de topacio blanco natural.", "Anillo Solitario 'Eternity Sparkle'", 120000.0, null, 20, anillos, null));
                productRepository.save(new Product(true, "Perlas cultivadas de agua dulce y broche marinero de plata dorada.", "Gargantilla Perlas 'Riviera'", 75000.0, null, 30, collares, null));

                System.out.println("Joyas de prueba cargadas exitosamente.");
            }

            if (usuarioRepository.findByEmailIgnoreCase("admin@aurea.com").isEmpty()) {
                System.out.println("Creando usuario Administrador por defecto...");
                Usuario admin = new Usuario(
                        "Admin",
                        "Aurea",
                        "admin@aurea.com",
                        passwordEncoder.encode("admin123"),
                        Rol.ADMIN
                );
                usuarioRepository.save(admin);
                System.out.println("Administrador creado exitosamente (email: admin@aurea.com | pass: admin123).");
            }
        };
    }
}
