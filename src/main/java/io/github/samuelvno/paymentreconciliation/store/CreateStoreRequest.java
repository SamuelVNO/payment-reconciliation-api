package io.github.samuelvno.paymentreconciliation.store;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "\\d{14}", message = "must be a CNPJ with 14 digits") String document,
        @NotNull @DecimalMin("0.0") @DecimalMax(value = "1.0", inclusive = false) @Digits(integer = 1, fraction = 4)
        BigDecimal feeRate) {
}
