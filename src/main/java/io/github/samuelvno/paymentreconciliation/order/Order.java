package io.github.samuelvno.paymentreconciliation.order;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;
import io.github.samuelvno.paymentreconciliation.store.Store;

// "order" é palavra reservada no SQL, por isso a tabela se chama "orders".
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    // @ManyToOne é EAGER por padrão: sempre declare LAZY.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(nullable = false, length = 64, updatable = false)
    private String externalId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate orderDate;

    // STRING, nunca ORDINAL: reordenar o enum corromperia os dados salvos.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    // Lock otimista: duas atualizações concorrentes não se sobrescrevem em silêncio.
    @Version
    private long version;

    protected Order() {
        // exigido pelo JPA
    }

    public Order(Store store, String externalId, BigDecimal amount, LocalDate orderDate) {
        this.store = store;
        this.externalId = externalId;
        this.amount = amount;
        this.orderDate = orderDate;
    }

    public void markAs(ReconciliationStatus status) {
        this.reconciliationStatus = status;
    }

    public Store getStore() {
        return store;
    }

    public String getExternalId() {
        return externalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }
}
