package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.ReconciliationStatus;
import io.github.samuelvno.paymentreconciliation.payout.Payout;
import io.github.samuelvno.paymentreconciliation.store.Store;

/**
 * Regras de conciliação de UM pedido. Sem Spring, sem banco: só entrada e saída,
 * por isso o teste unitário cobre todos os casos em milissegundos.
 */
class ReconciliationRules {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    record Finding(DiscrepancyType type, Payout payout, BigDecimal expected, BigDecimal actual, String description) {
    }

    record Evaluation(ReconciliationStatus status, List<Finding> findings) {
    }

    private final int settlementDays;
    private final BigDecimal feeTolerance;

    ReconciliationRules(int settlementDays, BigDecimal feeTolerance) {
        this.settlementDays = settlementDays;
        this.feeTolerance = feeTolerance;
    }

    Evaluation evaluate(Store store, Order order, List<Payout> payouts, LocalDate today) {
        if (payouts.isEmpty()) {
            LocalDate deadline = order.getOrderDate().plusDays(settlementDays);
            if (!today.isAfter(deadline)) {
                return new Evaluation(ReconciliationStatus.PENDING, List.of());
            }
            return new Evaluation(ReconciliationStatus.DIVERGENT, List.of(new Finding(
                    DiscrepancyType.MISSING_PAYOUT, null, order.getAmount(), ZERO,
                    "No payout received until settlement deadline " + deadline)));
        }

        // O repasse mais antigo é o "oficial"; os demais são pagamentos em dobro.
        List<Payout> sorted = payouts.stream()
                .sorted(Comparator.comparing(Payout::getPaidAt).thenComparing(Payout::getExternalId))
                .toList();
        Payout payout = sorted.getFirst();
        List<Finding> findings = new ArrayList<>();

        for (Payout extra : sorted.subList(1, sorted.size())) {
            findings.add(new Finding(DiscrepancyType.DUPLICATE_PAYOUT, extra, ZERO, extra.getGrossAmount(),
                    "Order already paid by payout " + payout.getExternalId()));
        }
        // compareTo, nunca equals: 150.0 e 150.00 são iguais em valor, mas equals diz que não
        if (payout.getGrossAmount().compareTo(order.getAmount()) != 0) {
            findings.add(new Finding(DiscrepancyType.AMOUNT_MISMATCH, payout, order.getAmount(),
                    payout.getGrossAmount(), "Gross amount paid differs from order amount"));
        }
        BigDecimal expectedFee = store.expectedFeeFor(payout.getGrossAmount());
        if (payout.getFeeAmount().subtract(expectedFee).compareTo(feeTolerance) > 0) {
            findings.add(new Finding(DiscrepancyType.FEE_OVERCHARGED, payout, expectedFee, payout.getFeeAmount(),
                    "Fee above contracted rate of " + store.getFeeRate()));
        }

        ReconciliationStatus status = findings.isEmpty() ? ReconciliationStatus.RECONCILED : ReconciliationStatus.DIVERGENT;
        return new Evaluation(status, List.copyOf(findings));
    }
}
