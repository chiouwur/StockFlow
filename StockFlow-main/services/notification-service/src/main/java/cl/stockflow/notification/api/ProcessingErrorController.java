package cl.stockflow.notification.api;

import cl.stockflow.notification.domain.ProcessingError;
import cl.stockflow.notification.domain.ProcessingErrorRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/processing-errors")
public class ProcessingErrorController {

    private final ProcessingErrorRepository repository;

    public ProcessingErrorController(ProcessingErrorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProcessingError> list() {
        return repository.findAll();
    }
}