package io.github.samuelvno.paymentreconciliation.store;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreResponse(UUID id, String name, String document, BigDecimal feeRate, Instant createdAt) {

    static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getDocument(), store.getFeeRate(),
                store.getCreatedAt());
    }
}
