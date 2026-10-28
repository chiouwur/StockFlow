package cl.stockflow.product.config;

import cl.stockflow.product.domain.Product;
import cl.stockflow.product.domain.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Datos de ejemplo solo para el perfil local.
@Configuration
@Profile("local")
class LocalDataSeeder {
    @Bean
    CommandLineRunner seed(ProductRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Product("TEC-001", "Teclado mecánico", 5));
                repo.save(new Product("MON-024", "Monitor 24 pulgadas", 6));
                repo.save(new Product("CAB-HDMI", "Cable HDMI 2 m", 20));
            }
        };
    }
}
