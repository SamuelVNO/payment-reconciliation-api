package io.github.samuelvno.paymentreconciliation.payout;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import io.github.samuelvno.paymentreconciliation.shared.error.InvalidRequestException;

/**
 * Lê o CSV de repasses do adquirente. Linhas inválidas não derrubam o arquivo:
 * viram RowError e o resto é importado.
 */
@Component
public class PayoutCsvParser {

    static final String[] HEADERS = {"payout_id", "order_id", "gross_amount", "fee_amount", "net_amount", "paid_at"};

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(HEADERS)
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .get();

    public record PayoutRow(String externalId, String orderExternalId, BigDecimal grossAmount,
                            BigDecimal feeAmount, BigDecimal netAmount, LocalDate paidAt) {
    }

    public record RowError(long line, String message) {
    }

    public record ParseResult(List<PayoutRow> rows, List<RowError> errors) {

        public int totalRows() {
            return rows.size() + errors.size();
        }
    }

    public ParseResult parse(InputStream input) {
        List<PayoutRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             CSVParser parser = FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                // +1 porque a linha 1 é o cabeçalho
                long line = record.getRecordNumber() + 1;
                try {
                    rows.add(toRow(record));
                } catch (IllegalArgumentException | DateTimeParseException ex) {
                    errors.add(new RowError(line, ex.getMessage()));
                }
            }
        } catch (IOException | UncheckedIOException ex) {
            throw new InvalidRequestException("Could not read CSV file: " + ex.getMessage());
        }
        return new ParseResult(rows, errors);
    }

    private PayoutRow toRow(CSVRecord record) {
        if (!record.isConsistent()) {
            throw new IllegalArgumentException("expected %d columns but got %d".formatted(HEADERS.length, record.size()));
        }
        String payoutId = required(record, "payout_id");
        String orderId = required(record, "order_id");
        BigDecimal gross = money(record, "gross_amount");
        BigDecimal fee = money(record, "fee_amount");
        BigDecimal net = money(record, "net_amount");
        LocalDate paidAt = LocalDate.parse(required(record, "paid_at"));

        if (gross.signum() <= 0) {
            throw new IllegalArgumentException("gross_amount must be positive");
        }
        if (fee.signum() < 0) {
            throw new IllegalArgumentException("fee_amount must not be negative");
        }
        // compareTo, não equals: 10.0 e 10.00 são o mesmo valor com escalas diferentes
        if (gross.subtract(fee).compareTo(net) != 0) {
            throw new IllegalArgumentException("net_amount %s != gross_amount - fee_amount".formatted(net));
        }
        return new PayoutRow(payoutId, orderId, gross, fee, net, paidAt);
    }

    private static String required(CSVRecord record, String column) {
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(column + " is required");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException(column + " must have at most 64 characters");
        }
        return value;
    }

    private static BigDecimal money(CSVRecord record, String column) {
        String value = required(record, column);
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.scale() > 2) {
                throw new IllegalArgumentException(column + " must have at most 2 decimal places");
            }
            return amount.setScale(2);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(column + " is not a valid number: " + value);
        }
    }
}
