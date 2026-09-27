package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.ReconciliationStatus;
import io.github.samuelvno.paymentreconciliation.payout.Payout;
import io.github.samuelvno.paymentreconciliation.payout.PayoutCsvParser.PayoutRow;
import io.github.samuelvno.paymentreconciliation.store.Store;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);
    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 9, 20);

    private final Store store = new Store("Loja", "12345678000190", new BigDecimal("0.0299"));
    private final ReconciliationRules rules = new ReconciliationRules(2, new BigDecimal("0.00"));

    @Test
    void reconcilesWhenAmountAndFeeMatch() {
        Order order = order("150.00", ORDER_DATE);

        var evaluation = rules.evaluate(store, order, List.of(payout("PAY-1", "150.00", "4.48")), TODAY);

        assertThat(evaluation.status()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(evaluation.findings()).isEmpty();
    }

    @Test
    void keepsPendingInsideSettlementWindow() {
        Order order = order("150.00", TODAY.minusDays(2));

        var evaluation = rules.evaluate(store, order, List.of(), TODAY);

        assertThat(evaluation.status()).isEqualTo(ReconciliationStatus.PENDING);
    }

    @Test
    void flagsMissingPayoutAfterSettlementWindow() {
        Order order = order("75.00", TODAY.minusDays(3));

        var evaluation = rules.evaluate(store, order, List.of(), TODAY);

        assertThat(evaluation.status()).isEqualTo(ReconciliationStatus.DIVERGENT);
        assertThat(evaluation.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(DiscrepancyType.MISSING_PAYOUT);
            assertThat(finding.expected()).isEqualByComparingTo("75.00");
            assertThat(finding.actual()).isEqualByComparingTo("0");
        });
    }

    @Test
    void flagsAmountMismatch() {
        var evaluation = rules.evaluate(store, order("200.00", ORDER_DATE),
                List.of(payout("PAY-1", "180.00", "5.38")), TODAY);

        assertThat(evaluation.findings()).extracting(ReconciliationRules.Finding::type)
                .containsExactly(DiscrepancyType.AMOUNT_MISMATCH);
    }

    @Test
    void flagsFeeAboveContractedRate() {
        // 89.90 * 2,99% = 2.69; o adquirente cobrou 3.50
        var evaluation = rules.evaluate(store, order("89.90", ORDER_DATE),
                List.of(payout("PAY-1", "89.90", "3.50")), TODAY);

        assertThat(evaluation.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(DiscrepancyType.FEE_OVERCHARGED);
            assertThat(finding.expected()).isEqualByComparingTo("2.69");
            assertThat(finding.actual()).isEqualByComparingTo("3.50");
        });
    }

    @Test
    void toleranceAbsorbsRoundingCents() {
        ReconciliationRules tolerant = new ReconciliationRules(2, new BigDecimal("0.01"));

        var evaluation = tolerant.evaluate(store, order("150.00", ORDER_DATE),
                List.of(payout("PAY-1", "150.00", "4.49")), TODAY);

        assertThat(evaluation.status()).isEqualTo(ReconciliationStatus.RECONCILED);
    }

    @Test
    void flagsEveryExtraPayoutAsDuplicate() {
        var evaluation = rules.evaluate(store, order("120.00", ORDER_DATE), List.of(
                payout("PAY-3", "120.00", "3.59"),
                payout("PAY-1", "120.00", "3.59"),
                payout("PAY-2", "120.00", "3.59")), TODAY);

        assertThat(evaluation.status()).isEqualTo(ReconciliationStatus.DIVERGENT);
        assertThat(evaluation.findings())
                .extracting(finding -> finding.payout().getExternalId())
                .containsExactly("PAY-2", "PAY-3");
    }

    private Order order(String amount, LocalDate date) {
        return new Order(store, "PED-1", new BigDecimal(amount), date);
    }

    private Payout payout(String id, String gross, String fee) {
        BigDecimal grossAmount = new BigDecimal(gross);
        BigDecimal feeAmount = new BigDecimal(fee);
        return new Payout(store, null, new PayoutRow(id, "PED-1", grossAmount, feeAmount,
                grossAmount.subtract(feeAmount), ORDER_DATE.plusDays(1)));
    }
}
