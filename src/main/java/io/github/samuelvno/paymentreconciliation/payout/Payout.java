package io.github.samuelvno.paymentreconciliation.payout;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;
import io.github.samuelvno.paymentreconciliation.store.Store;

/** Repasse efetivamente pago pelo adquirente para um pedido. */
@Entity
@Table(name = "payouts")
public class Payout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false, updatable = false)
    private PayoutImport payoutImport;

    @Column(nullable = false, length = 64, updatable = false)
    private String externalId;

    @Column(nullable = false, length = 64, updatable = false)
    private String orderExternalId;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal netAmount;

    @Column(nullable = false, updatable = false)
    private LocalDate paidAt;

    protected Payout() {
        // exigido pelo JPA
    }

    public Payout(Store store, PayoutImport payoutImport, PayoutCsvParser.PayoutRow row) {
        this.store = store;
        this.payoutImport = payoutImport;
        this.externalId = row.externalId();
        this.orderExternalId = row.orderExternalId();
        this.grossAmount = row.grossAmount();
        this.feeAmount = row.feeAmount();
        this.netAmount = row.netAmount();
        this.paidAt = row.paidAt();
    }

    public Store getStore() {
        return store;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getOrderExternalId() {
        return orderExternalId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public LocalDate getPaidAt() {
        return paidAt;
    }
}
