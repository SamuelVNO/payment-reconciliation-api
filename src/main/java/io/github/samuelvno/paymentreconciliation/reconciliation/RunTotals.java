package io.github.samuelvno.paymentreconciliation.reconciliation;

/** Placar imutável de um lote; somado lote a lote até fechar o run. */
record RunTotals(int ordersChecked, int reconciled, int divergent, int stillPending, int discrepancies) {

    static final RunTotals EMPTY = new RunTotals(0, 0, 0, 0, 0);

    RunTotals plus(RunTotals other) {
        return new RunTotals(ordersChecked + other.ordersChecked, reconciled + other.reconciled,
                divergent + other.divergent, stillPending + other.stillPending,
                discrepancies + other.discrepancies);
    }
}
