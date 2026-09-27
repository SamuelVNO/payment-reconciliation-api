package io.github.samuelvno.paymentreconciliation.store;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;

@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 14, unique = true)
    private String document;

    // Taxa contratada com o adquirente, ex.: 0.0299 = 2,99%.
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    protected Store() {
        // exigido pelo JPA
    }

    public Store(String name, String document, BigDecimal feeRate) {
        this.name = name;
        this.document = document;
        this.feeRate = feeRate;
    }

    public void update(String name, BigDecimal feeRate) {
        this.name = name;
        this.feeRate = feeRate;
    }

    /** Taxa que o adquirente deveria cobrar sobre um valor bruto. */
    public BigDecimal expectedFeeFor(BigDecimal grossAmount) {
        return grossAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }
}
