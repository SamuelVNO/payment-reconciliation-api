package io.github.samuelvno.paymentreconciliation.payout;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutCsvParserTest {

    private final PayoutCsvParser parser = new PayoutCsvParser();

    @Test
    void parsesValidRowsAndCollectsErrorsPerLine() {
        String csv = """
                payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at
                PAY-1,PED-1,150.00,4.48,145.52,2026-09-22
                PAY-2,PED-2,89.9,2.69,87.21,2026-09-22
                PAY-3,PED-3,abc,1.00,1.00,2026-09-22
                PAY-4,PED-4,10.00,1.00,8.00,2026-09-22
                PAY-5,,10.00,1.00,9.00,2026-09-22
                PAY-6,PED-6,10.001,1.00,9.001,2026-09-22
                """;

        PayoutCsvParser.ParseResult result = parser.parse(stream(csv));

        assertThat(result.totalRows()).isEqualTo(6);
        assertThat(result.rows()).extracting(PayoutCsvParser.PayoutRow::externalId).containsExactly("PAY-1", "PAY-2");
        assertThat(result.rows().get(1).grossAmount()).hasToString("89.90");
        assertThat(result.rows().get(0).paidAt()).isEqualTo(LocalDate.of(2026, 9, 22));
        assertThat(result.errors()).extracting(PayoutCsvParser.RowError::line).containsExactly(4L, 5L, 6L, 7L);
        assertThat(result.errors().get(1).message()).contains("net_amount");
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
