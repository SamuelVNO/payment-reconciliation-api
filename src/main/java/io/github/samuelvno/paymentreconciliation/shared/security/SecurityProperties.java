package io.github.samuelvno.paymentreconciliation.shared.security;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Admin admin) {

    // HS256 exige chave de pelo menos 256 bits (32 bytes)
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotBlank String issuer, @NotNull Duration ttl) {
    }

    public record Admin(String email, String password) {

        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
