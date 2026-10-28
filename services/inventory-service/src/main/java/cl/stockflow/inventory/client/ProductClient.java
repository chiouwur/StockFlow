package cl.stockflow.inventory.client;

import cl.stockflow.inventory.api.ApiException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Pregunta a product-service si un producto existe y está activo (sin tocar su base de datos).
@Component
public class ProductClient {

    private final RestClient rest;

    public ProductClient(RestClient.Builder builder, @Value("${app.product-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.rest = builder.requestFactory(factory).baseUrl(baseUrl).build();
    }

    public ProductInfo get(UUID id) {
        try {
            return rest.get()
                    .uri("/api/products/{id}", id)
                    .headers(h -> {
                        String token = currentToken();
                        if (token != null) h.setBearerAuth(token);   // reenvía el token del usuario
                    })
                    .retrieve()
                    .body(ProductInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "product-service no está disponible");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo validar el producto");
        }
    }

    private static String currentToken() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a instanceof JwtAuthenticationToken j ? j.getToken().getTokenValue() : null;
    }
}
