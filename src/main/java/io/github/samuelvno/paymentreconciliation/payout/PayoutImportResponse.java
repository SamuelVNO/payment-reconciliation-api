package io.github.samuelvno.paymentreconciliation.payout;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PayoutImportResponse(
        UUID id,
        String idempotencyKey,
        String fileName,
        int totalRows,
        int importedRows,
        int duplicateRows,
        int rejectedRows,
        List<PayoutCsvParser.RowError> errors,
        Instant createdAt) {

    static PayoutImportResponse from(PayoutImport payoutImport) {
        return new PayoutImportResponse(payoutImport.getId(), payoutImport.getIdempotencyKey(),
                payoutImport.getFileName(), payoutImport.getTotalRows(), payoutImport.getImportedRows(),
                payoutImport.getDuplicateRows(), payoutImport.getRejectedRows(), payoutImport.getErrors(),
                payoutImport.getCreatedAt());
    }
}
