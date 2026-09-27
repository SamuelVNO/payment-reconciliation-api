package io.github.samuelvno.paymentreconciliation.payout;

import java.nio.file.Path;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração tipada de "app.payout-inbox.*". Se faltar valor, a app nem sobe (fail fast).
 */
@Validated
@ConfigurationProperties(prefix = "app.payout-inbox")
public record PayoutInboxProperties(@NotNull Path directory, @NotBlank String cron) {
}
