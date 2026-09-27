package io.github.samuelvno.paymentreconciliation.shared.outbox;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(@NotNull Duration pollInterval, @Min(1) @Max(1000) int batchSize) {
}
