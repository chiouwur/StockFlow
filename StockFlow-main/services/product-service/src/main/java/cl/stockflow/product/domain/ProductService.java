package cl.stockflow.product.domain;

import cl.stockflow.product.api.ApiException;
import cl.stockflow.product.api.ProductRequest;
import cl.stockflow.product.api.ProductResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(String search) {
        String s = search == null ? "" : search.trim();
        List<Product> found = s.isEmpty()
                ? repo.findAllByOrderByNameAsc()
                : repo.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(s, s);
        return found.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest r) {
        String code = r.code().trim();
        if (repo.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un producto con ese código");
        }
        return ProductResponse.from(repo.save(new Product(code, r.name().trim(), r.minimumStock())));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest r) {
        Product p = find(id);
        String code = r.code().trim();
        if (repo.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un producto con ese código");
        }
        p.setCode(code);
        p.setName(r.name().trim());
        p.setMinimumStock(r.minimumStock());
        return ProductResponse.from(p);
    }

    @Transactional
    public ProductResponse deactivate(UUID id) {
        Product p = find(id);
        p.setActive(false);
        return ProductResponse.from(p);
    }

    private Product find(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }
}
