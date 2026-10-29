package cl.stockflow.rabbitmqadmin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/admin/rabbitmq/**"
                        ).hasAuthority("SCOPE_inventory.access")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.issuer-uri:}") String issuerUri,
            @Value("${app.security.jwk-set-uri:}") String jwkSetUri,
            @Value("${app.security.audience:}") String audience) {

        JwtDecoder decoder;

        if (!jwkSetUri.isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (!issuerUri.isBlank()) {
            decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        } else {
            throw new IllegalStateException(
                    "Configura JWT_ISSUER o JWT_JWK_SET_URI."
            );
        }

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();

        if (!issuerUri.isBlank()) {
            validators.add(
                    JwtValidators.createDefaultWithIssuer(issuerUri)
            );
        } else {
            validators.add(JwtValidators.createDefault());
        }

        if (!audience.isBlank()) {
            validators.add(jwt -> jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    "invalid_token",
                                    "Audiencia no válida",
                                    null
                            )
                    ));
        }

        decoder = withValidators(decoder, validators);
        return decoder;
    }

    private JwtDecoder withValidators(
            JwtDecoder original,
            List<OAuth2TokenValidator<Jwt>> validators) {

        OAuth2TokenValidator<Jwt> combined = jwt -> {
            for (OAuth2TokenValidator<Jwt> validator : validators) {
                OAuth2TokenValidatorResult result = validator.validate(jwt);
                if (result.hasErrors()) {
                    return result;
                }
            }
            return OAuth2TokenValidatorResult.success();
        };

        if (original instanceof NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(combined);
        }

        return original;
    }
}