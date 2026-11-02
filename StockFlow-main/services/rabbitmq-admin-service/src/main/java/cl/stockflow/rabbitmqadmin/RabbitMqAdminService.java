
package cl.stockflow.rabbitmqadmin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RabbitMqAdminService {

    private final RestClient client;

    public RabbitMqAdminService(
            @Value("${app.rabbitmq.management-url}") String baseUrl,
            @Value("${app.rabbitmq.username}") String username,
            @Value("${app.rabbitmq.password}") String password) {

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password))
                .defaultHeader(
                        "Accept",
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> queues() {
        return client.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/queues/{vhost}")
                                .build("/"))
                .retrieve()
                .body(List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> exchanges() {
        return client.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/exchanges/{vhost}")
                                .build("/"))
                .retrieve()
                .body(List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> bindings() {
        return client.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/bindings/{vhost}")
                                .build("/"))
                .retrieve()
                .body(List.class);
    }
}
