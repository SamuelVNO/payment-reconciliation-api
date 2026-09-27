package io.github.samuelvno.paymentreconciliation.reconciliation;

public enum DiscrepancyType {
    /** Prazo de liquidação venceu e nenhum repasse chegou. */
    MISSING_PAYOUT,
    /** Valor bruto repassado diferente do valor do pedido. */
    AMOUNT_MISMATCH,
    /** Adquirente cobrou taxa maior que a contratada. */
    FEE_OVERCHARGED,
    /** Mesmo pedido pago mais de uma vez. */
    DUPLICATE_PAYOUT,
    /** Repasse que não bate com nenhum pedido da loja. */
    ORPHAN_PAYOUT
}
