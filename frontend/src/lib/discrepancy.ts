import type { DiscrepancyType, ReconciliationStatus } from "./types"

type TypeInfo = {
  label: string
  explain: string
  /** loss = a loja recebeu menos; surplus = entrou dinheiro que não bate com nada */
  tone: "loss" | "surplus"
}

export const DISCREPANCY: Record<DiscrepancyType, TypeInfo> = {
  FEE_OVERCHARGED: { label: "Taxa acima", explain: "Adquirente cobrou mais que a taxa contratada", tone: "loss" },
  AMOUNT_MISMATCH: { label: "Valor divergente", explain: "Bruto repassado diferente do valor do pedido", tone: "loss" },
  MISSING_PAYOUT: { label: "Repasse faltando", explain: "Prazo de liquidação venceu sem repasse", tone: "loss" },
  DUPLICATE_PAYOUT: { label: "Pago em dobro", explain: "O mesmo pedido recebeu mais de um repasse", tone: "surplus" },
  ORPHAN_PAYOUT: { label: "Repasse órfão", explain: "Repasse sem pedido correspondente na loja", tone: "surplus" },
}

export const DISCREPANCY_TYPES = Object.keys(DISCREPANCY) as DiscrepancyType[]

export const ORDER_STATUS: Record<ReconciliationStatus, { label: string; tone: "ok" | "loss" | "pending" }> = {
  RECONCILED: { label: "Conciliado", tone: "ok" },
  DIVERGENT: { label: "Divergente", tone: "loss" },
  PENDING: { label: "No prazo", tone: "pending" },
}

/**
 * Efeito no caixa da loja. A diferença da API é sempre "recebido − esperado" do valor
 * comparado; na taxa, recebido é a taxa cobrada, então cobrar a mais é perda para a loja.
 */
export function impact(type: DiscrepancyType, difference: number) {
  return type === "FEE_OVERCHARGED" ? -difference : difference
}
