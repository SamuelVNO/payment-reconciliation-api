package io.github.samuelvno.paymentreconciliation.payout;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;
import io.github.samuelvno.paymentreconciliation.store.Store;

/** Registro de cada upload de CSV. A dupla (loja, Idempotency-Key) é única. */
@Entity
@Table(name = "payout_imports")
public class PayoutImport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private String fileName;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int importedRows;

    @Column(nullable = false)
    private int duplicateRows;

    @Column(nullable = false)
    private int rejectedRows;

    // Hibernate 6 mapeia a lista de records direto para JSONB.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<PayoutCsvParser.RowError> errors = new ArrayList<>();

    protected PayoutImport() {
        // exigido pelo JPA
    }

    PayoutImport(Store store, String idempotencyKey, String fileName) {
        this.store = store;
        this.idempotencyKey = idempotencyKey;
        this.fileName = fileName;
    }

    void summarize(int totalRows, int importedRows, int duplicateRows, List<PayoutCsvParser.RowError> errors) {
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.duplicateRows = duplicateRows;
        this.rejectedRows = errors.size();
        this.errors = new ArrayList<>(errors);
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public int getRejectedRows() {
        return rejectedRows;
    }

    public List<PayoutCsvParser.RowError> getErrors() {
        return errors;
    }
}
