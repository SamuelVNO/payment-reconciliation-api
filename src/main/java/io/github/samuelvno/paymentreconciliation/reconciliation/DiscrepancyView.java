package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projeção de leitura: o JPQL monta este record direto do SELECT com JOINs,
 * sem carregar entidades nem disparar lazy loading (zero N+1).
 */
public record DiscrepancyView(
        UUID id,
        DiscrepancyType type,
        DiscrepancyStatus status,
        String orderExternalId,
        String payoutExternalId,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        String description,
        String resolutionNote,
        Instant detectedAt,
        Instant resolvedAt) {
}
