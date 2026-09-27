package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.reconciliation")
public record ReconciliationProperties(
        @NotBlank String cron,
        @Min(1) @Max(5000) int batchSize,
        // D+N: quantos dias o adquirente tem para pagar antes de virar MISSING_PAYOUT
        @PositiveOrZero int settlementDays,
        @NotNull @PositiveOrZero BigDecimal feeTolerance) {
}
