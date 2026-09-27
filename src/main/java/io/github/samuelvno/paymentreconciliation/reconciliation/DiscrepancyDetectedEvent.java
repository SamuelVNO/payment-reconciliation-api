package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato do evento publicado no Kafka. Mudar um campo aqui é mudar a API de outro time. */
public record DiscrepancyDetectedEvent(
        UUID discrepancyId,
        UUID storeId,
        DiscrepancyType type,
        String orderExternalId,
        String payoutExternalId,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        String description,
        Instant detectedAt) {

    public static final String EVENT_TYPE = "DiscrepancyDetected";

    static DiscrepancyDetectedEvent from(UUID storeId, Discrepancy discrepancy) {
        return new DiscrepancyDetectedEvent(discrepancy.getId(), storeId, discrepancy.getType(),
                discrepancy.getOrder() == null ? null : discrepancy.getOrder().getExternalId(),
                discrepancy.getPayout() == null ? null : discrepancy.getPayout().getExternalId(),
                discrepancy.getExpectedAmount(), discrepancy.getActualAmount(), discrepancy.getDifference(),
                discrepancy.getDescription(), discrepancy.getCreatedAt());
    }
}
