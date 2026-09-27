package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.payout.Payout;
import io.github.samuelvno.paymentreconciliation.shared.error.BusinessConflictException;
import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;
import io.github.samuelvno.paymentreconciliation.store.Store;

@Entity
@Table(name = "discrepancies")
public class Discrepancy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private ReconciliationRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", updatable = false)
    private Payout payout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private DiscrepancyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscrepancyStatus status = DiscrepancyStatus.OPEN;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal expectedAmount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal actualAmount;

    // actual - expected: negativo = loja recebeu menos do que devia
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal difference;

    @Column(nullable = false, updatable = false)
    private String description;

    private String resolutionNote;

    private Instant resolvedAt;

    @Version
    private long version;

    protected Discrepancy() {
        // exigido pelo JPA
    }

    private Discrepancy(Store store, ReconciliationRun run, Order order, Payout payout, DiscrepancyType type,
                        BigDecimal expectedAmount, BigDecimal actualAmount, String description) {
        this.store = store;
        this.run = run;
        this.order = order;
        this.payout = payout;
        this.type = type;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.difference = actualAmount.subtract(expectedAmount);
        this.description = description;
    }

    static Discrepancy of(Store store, ReconciliationRun run, Order order, ReconciliationRules.Finding finding) {
        return new Discrepancy(store, run, order, finding.payout(), finding.type(), finding.expected(),
                finding.actual(), finding.description());
    }

    static Discrepancy orphan(Store store, ReconciliationRun run, Payout payout) {
        return new Discrepancy(store, run, null, payout, DiscrepancyType.ORPHAN_PAYOUT,
                BigDecimal.ZERO.setScale(2), payout.getGrossAmount(),
                "No order %s found for this store".formatted(payout.getOrderExternalId()));
    }

    void resolve(String note, Instant when) {
        if (status == DiscrepancyStatus.RESOLVED) {
            throw new BusinessConflictException("Discrepancy %s is already resolved".formatted(getId()));
        }
        this.status = DiscrepancyStatus.RESOLVED;
        this.resolutionNote = note;
        this.resolvedAt = when;
    }

    public DiscrepancyType getType() {
        return type;
    }

    public DiscrepancyStatus getStatus() {
        return status;
    }

    public Order getOrder() {
        return order;
    }

    public Payout getPayout() {
        return payout;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public String getDescription() {
        return description;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
