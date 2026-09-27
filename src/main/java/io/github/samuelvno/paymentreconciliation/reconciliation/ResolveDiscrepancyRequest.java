package io.github.samuelvno.paymentreconciliation.reconciliation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveDiscrepancyRequest(@NotBlank @Size(max = 255) String note) {
}
