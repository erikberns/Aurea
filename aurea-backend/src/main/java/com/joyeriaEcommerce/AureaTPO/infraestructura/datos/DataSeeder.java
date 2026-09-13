package com.joyeriaEcommerce.AureaTPO.infraestructura.datos;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.productos.datos.ProductDAO;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Category;
import com.joyeriaEcommerce.AureaTPO.productos.datos.CategoryRepository;
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
    public CommandLineRunner loadData(ProductDAO productDAO, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, CategoryRepository categoryRepository) {
        return args -> {
            if (productDAO.count() == 0) {
                System.out.println("Cargando joyas de prueba en la base de datos...");

                Category anillos = categoryRepository.save(new Category("Anillos apilables"));
                Category collares = categoryRepository.save(new Category("Collares & Medallas"));
                Category pulseras = categoryRepository.save(new Category("Pulseras & Eslabones"));
                Category pendientes = categoryRepository.save(new Category("Pendientes & Huggies"));

                productDAO.save(new Product(true, "Plata de Ley 925 con circonitas engastadas a mano.", "Anillo Apilable 'Luna Nueva'", 45000.0, "https://images.unsplash.com/photo-1605100804763-247f67b454e6?q=80&w=800&auto=format&fit=crop", 100, anillos));
                productDAO.save(new Product(true, "Baño de Oro 18k con cadena ajustable (40–45 cm).", "Collar Medalla 'Astro Solar'", 89000.0, "https://images.unsplash.com/photo-1599643478524-fb66f456b825?q=80&w=800&auto=format&fit=crop", 50, collares));
                productDAO.save(new Product(true, "Plata de Ley 925 rodiada antidesgaste de brillo espejo.", "Pulsera Eslabones 'Aura Link'", 65000.0, "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800&auto=format&fit=crop", 75, pulseras));
                productDAO.save(new Product(true, "Plata 925 con baño de Oro 18k y cierre click seguro.", "Pendientes Aros 'Demi Huggies'", 39000.0, "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?q=80&w=800&auto=format&fit=crop", 200, pendientes));
                productDAO.save(new Product(true, "Oro Vermeil 18k con gema central de topacio blanco natural.", "Anillo Solitario 'Eternity Sparkle'", 120000.0, "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?q=80&w=800&auto=format&fit=crop", 20, anillos));
                productDAO.save(new Product(true, "Perlas cultivadas de agua dulce y broche marinero de plata dorada.", "Gargantilla Perlas 'Riviera'", 75000.0, "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?q=80&w=800&auto=format&fit=crop", 30, collares));

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
