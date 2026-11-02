package cl.stockflow.product.config;

import cl.stockflow.product.domain.Product;
import cl.stockflow.product.domain.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoProductInitializer {

    @Bean
    CommandLineRunner initializeDemoProducts(
            ProductRepository productRepository) {

        return args -> {
            // CATEGORÍA 1: Ollas
            createIfMissing(productRepository, "OLL-001", "Olla de aluminio", 5);
            createIfMissing(productRepository, "OLL-002", "Olla a presión", 3);
            createIfMissing(productRepository, "OLL-003", "Olla de acero inoxidable", 5);
            createIfMissing(productRepository, "OLL-004", "Juego de ollas", 3);

            // CATEGORÍA 2: Sartenes y planchas
            createIfMissing(productRepository, "SAR-001", "Sartén antiadherente", 5);
            createIfMissing(productRepository, "SAR-002", "Sartén de hierro", 3);
            createIfMissing(productRepository, "SAR-003", "Panquequera", 4);
            createIfMissing(productRepository, "SAR-004", "Plancha para asar", 3);

            // CATEGORÍA 3: Utensilios de cocina
            createIfMissing(productRepository, "UTE-001", "Cucharón de cocina", 10);
            createIfMissing(productRepository, "UTE-002", "Espátula de cocina", 10);
            createIfMissing(productRepository, "UTE-003", "Espumadera", 8);
            createIfMissing(productRepository, "UTE-004", "Batidor manual", 8);

            // CATEGORÍA 4: Accesorios y almacenamiento
            createIfMissing(productRepository, "ACC-001", "Colador de acero inoxidable", 5);
            createIfMissing(productRepository, "ACC-002", "Rallador de cocina", 5);
            createIfMissing(productRepository, "ACC-003", "Recipiente plástico", 8);
            createIfMissing(productRepository, "ACC-004", "Tabla para picar", 5);

            System.out.println(
                    "Inicialización de productos de demostración finalizada.");
        };
    }

    private void createIfMissing(
            ProductRepository repository,
            String code,
            String name,
            int minimumStock) {

        if (!repository.existsByCodeIgnoreCase(code)) {
            repository.save(new Product(code, name, minimumStock));
            System.out.println("Producto creado: " + code + " - " + name);
        }
    }
}
