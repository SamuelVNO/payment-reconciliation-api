package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;
import io.github.samuelvno.paymentreconciliation.store.Store;

/** Uma execução do motor para uma loja: quem disparou, quando e o placar. */
@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TriggerSource triggerSource;

    @Column(nullable = false, updatable = false)
    private LocalDate businessDate;

    private int ordersChecked;
    private int reconciled;
    private int divergent;
    private int stillPending;
    private int discrepanciesFound;
    private Instant finishedAt;

    protected ReconciliationRun() {
        // exigido pelo JPA
    }

    ReconciliationRun(Store store, TriggerSource triggerSource, LocalDate businessDate) {
        this.store = store;
        this.triggerSource = triggerSource;
        this.businessDate = businessDate;
    }

    void finish(RunTotals totals, Instant finishedAt) {
        this.ordersChecked = totals.ordersChecked();
        this.reconciled = totals.reconciled();
        this.divergent = totals.divergent();
        this.stillPending = totals.stillPending();
        this.discrepanciesFound = totals.discrepancies();
        this.finishedAt = finishedAt;
    }

    public TriggerSource getTriggerSource() {
        return triggerSource;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public int getOrdersChecked() {
        return ordersChecked;
    }

    public int getReconciled() {
        return reconciled;
    }

    public int getDivergent() {
        return divergent;
    }

    public int getStillPending() {
        return stillPending;
    }

    public int getDiscrepanciesFound() {
        return discrepanciesFound;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
