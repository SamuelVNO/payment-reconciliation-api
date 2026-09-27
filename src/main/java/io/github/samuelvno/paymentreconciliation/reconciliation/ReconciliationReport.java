package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.github.samuelvno.paymentreconciliation.order.ReconciliationStatus;

public record ReconciliationReport(
        LocalDate from,
        LocalDate to,
        Map<ReconciliationStatus, Long> ordersByStatus,
        List<TypeSummary> discrepanciesByType,
        long openDiscrepancies) {

    /** Linha do GROUP BY: quantas divergências de cada tipo e o impacto em R$. */
    public record TypeSummary(DiscrepancyType type, long count, BigDecimal totalDifference) {
    }
}
