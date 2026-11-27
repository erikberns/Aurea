package com.joyeriaEcommerce.AureaTPO.infraestructura.datos;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductDAO;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Category;
import com.joyeriaEcommerce.AureaTPO.productos.datos.CategoryRepository;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Rol;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner loadData(ProductDAO productDAO, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, CategoryRepository categoryRepository,
                                      @Value("${app.admin.email:admin@aurea.com}") String adminEmail,
                                      @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            if (productDAO.count() == 0) {
                log.info("Cargando joyas de prueba en la base de datos...");

                Category anillos = categoryRepository.save(new Category("Anillos apilables"));
                Category collares = categoryRepository.save(new Category("Collares & Medallas"));
                Category pulseras = categoryRepository.save(new Category("Pulseras & Eslabones"));
                Category pendientes = categoryRepository.save(new Category("Pendientes & Huggies"));

                productDAO.save(new Product(true, "Plata de Ley 925 con circonitas engastadas a mano.", "Anillo Apilable 'Luna Nueva'", 45000.0, "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop", 100, anillos));
                productDAO.save(new Product(true, "Baño de Oro 18k con cadena ajustable (40–45 cm).", "Collar Medalla 'Astro Solar'", 89000.0, "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop", 50, collares));
                productDAO.save(new Product(true, "Plata de Ley 925 rodiada antidesgaste de brillo espejo.", "Pulsera Eslabones 'Aura Link'", 65000.0, "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop", 75, pulseras));
                productDAO.save(new Product(true, "Plata 925 con baño de Oro 18k y cierre click seguro.", "Pendientes Aros 'Demi Huggies'", 39000.0, "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?q=80&w=800&auto=format&fit=crop", 200, pendientes));
                productDAO.save(new Product(true, "Oro Vermeil 18k con gema central de topacio blanco natural.", "Anillo Solitario 'Eternity Sparkle'", 120000.0, "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop", 20, anillos));
                productDAO.save(new Product(true, "Perlas cultivadas de agua dulce y broche marinero de plata dorada.", "Gargantilla Perlas 'Riviera'", 75000.0, "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?q=80&w=800&auto=format&fit=crop", 30, collares));

                log.info("Joyas de prueba cargadas.");
            }

            // El administrador inicial toma sus credenciales de la configuración (ADMIN_EMAIL y ADMIN_PASSWORD).
            // La contraseña nunca se escribe en el código ni se muestra en el log.
            if (usuarioRepository.findByEmailIgnoreCase(adminEmail).isEmpty()) {
                if (adminPassword == null || adminPassword.isBlank()) {
                    log.warn("No se creó el administrador inicial: falta ADMIN_PASSWORD en la configuración.");
                } else {
                    usuarioRepository.save(new Usuario(
                            "Admin",
                            "Aurea",
                            adminEmail,
                            passwordEncoder.encode(adminPassword),
                            Rol.ADMIN
                    ));
                    log.info("Administrador inicial creado: {}", adminEmail);
                }
            }
        };
    }
}
