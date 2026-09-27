package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconciliationRunResponse(
        UUID id,
        TriggerSource triggerSource,
        LocalDate businessDate,
        int ordersChecked,
        int reconciled,
        int divergent,
        int stillPending,
        int discrepanciesFound,
        Instant startedAt,
        Instant finishedAt) {

    static ReconciliationRunResponse from(ReconciliationRun run) {
        return new ReconciliationRunResponse(run.getId(), run.getTriggerSource(), run.getBusinessDate(),
                run.getOrdersChecked(), run.getReconciled(), run.getDivergent(), run.getStillPending(),
                run.getDiscrepanciesFound(), run.getCreatedAt(), run.getFinishedAt());
    }
}
