package io.github.samuelvno.paymentreconciliation.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreAlertResponse(UUID id, UUID discrepancyId, String type, BigDecimal difference, String message,
                                 Instant createdAt) {

    static StoreAlertResponse from(StoreAlert alert) {
        return new StoreAlertResponse(alert.getId(), alert.getDiscrepancyId(), alert.getType(), alert.getDifference(),
                alert.getMessage(), alert.getCreatedAt());
    }
}
