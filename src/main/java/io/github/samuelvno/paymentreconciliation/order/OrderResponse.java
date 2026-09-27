package io.github.samuelvno.paymentreconciliation.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String externalId,
        BigDecimal amount,
        LocalDate orderDate,
        ReconciliationStatus reconciliationStatus,
        Instant createdAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getExternalId(), order.getAmount(), order.getOrderDate(),
                order.getReconciliationStatus(), order.getCreatedAt());
    }
}
