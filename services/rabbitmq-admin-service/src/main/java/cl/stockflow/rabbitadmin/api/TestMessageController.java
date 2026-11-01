package cl.stockflow.rabbitadmin.api;

import cl.stockflow.rabbitadmin.domain.TestMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/rabbit")
public class TestMessageController {

    private final TestMessageService testMessageService;

    public TestMessageController(TestMessageService testMessageService) {
        this.testMessageService = testMessageService;
    }

    @PostMapping("/test-messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendTestMessage(
            @Valid @RequestBody TestMessageRequest request
    ) {
        try {
            testMessageService.send(request.kind());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage()
            );
        }
    }

    public record TestMessageRequest(
            @NotBlank String kind
    ) {}
}