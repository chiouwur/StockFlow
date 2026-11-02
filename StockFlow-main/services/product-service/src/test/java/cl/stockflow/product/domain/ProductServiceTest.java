package cl.stockflow.product.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.stockflow.product.api.ApiException;
import cl.stockflow.product.api.ProductRequest;
import cl.stockflow.product.api.ProductResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// Pruebas unitarias de las reglas del catálogo. No usan base de datos ni red.
class ProductServiceTest {

    private ProductRepository repo;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductRepository.class);
        service = new ProductService(repo);
        when(repo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createTrimsFieldsAndActivatesProduct() {
        ProductResponse r = service.create(new ProductRequest("  TEC-1 ", " Teclado ", 5));

        assertEquals("TEC-1", r.code());
        assertEquals("Teclado", r.name());
        assertEquals(5, r.minimumStock());
        assertTrue(r.active());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repo.existsByCodeIgnoreCase("TEC-1")).thenReturn(true);

        ApiException e = assertThrows(ApiException.class,
                () -> service.create(new ProductRequest("TEC-1", "Teclado", 5)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(repo, never()).save(any(Product.class));
    }

    @Test
    void updateChangesEditableFields() {
        Product p = new Product("A", "Uno", 1);
        when(repo.findById(p.getId())).thenReturn(Optional.of(p));

        ProductResponse r = service.update(p.getId(), new ProductRequest("B", "Dos", 9));

        assertEquals("B", r.code());
        assertEquals("Dos", r.name());
        assertEquals(9, r.minimumStock());
    }

    @Test
    void updateRejectsCodeUsedByAnotherProduct() {
        Product p = new Product("A", "Uno", 1);
        when(repo.findById(p.getId())).thenReturn(Optional.of(p));
        when(repo.existsByCodeIgnoreCaseAndIdNot("B", p.getId())).thenReturn(true);

        ApiException e = assertThrows(ApiException.class,
                () -> service.update(p.getId(), new ProductRequest("B", "Uno", 1)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void deactivateMarksProductAsInactive() {
        Product p = new Product("A", "Uno", 1);
        when(repo.findById(p.getId())).thenReturn(Optional.of(p));

        ProductResponse r = service.deactivate(p.getId());

        assertFalse(r.active());
    }

    @Test
    void getUnknownProductReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> service.get(id));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
