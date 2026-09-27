package io.github.samuelvno.paymentreconciliation.notification;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;

@Entity
@Table(name = "store_alerts")
public class StoreAlert extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID storeId;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID discrepancyId;

    @Column(nullable = false, length = 30, updatable = false)
    private String type;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal difference;

    @Column(nullable = false, updatable = false)
    private String message;

    protected StoreAlert() {
        // exigido pelo JPA
    }

    StoreAlert(UUID storeId, UUID eventId, UUID discrepancyId, String type, BigDecimal difference, String message) {
        this.storeId = storeId;
        this.eventId = eventId;
        this.discrepancyId = discrepancyId;
        this.type = type;
        this.difference = difference;
        this.message = message;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getDiscrepancyId() {
        return discrepancyId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public String getMessage() {
        return message;
    }
}
