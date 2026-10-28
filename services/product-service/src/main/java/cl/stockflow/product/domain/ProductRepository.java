package cl.stockflow.product.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(String name, String code);
}
