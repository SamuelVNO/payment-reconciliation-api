// Espelho dos DTOs da API (records Java). BigDecimal chega como number no JSON.

export type Page<T> = {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export type ReconciliationStatus = "PENDING" | "RECONCILED" | "DIVERGENT"
export type DiscrepancyType =
  | "MISSING_PAYOUT"
  | "AMOUNT_MISMATCH"
  | "FEE_OVERCHARGED"
  | "DUPLICATE_PAYOUT"
  | "ORPHAN_PAYOUT"
export type DiscrepancyStatus = "OPEN" | "RESOLVED"
export type Role = "ADMIN" | "STORE_USER"

export type Store = {
  id: string
  name: string
  document: string
  feeRate: number
  createdAt: string
}

export type Order = {
  id: string
  externalId: string
  amount: number
  orderDate: string
  reconciliationStatus: ReconciliationStatus
  createdAt: string
}

export type Payout = {
  id: string
  externalId: string
  orderExternalId: string
  grossAmount: number
  feeAmount: number
  netAmount: number
  paidAt: string
}

export type PayoutImport = {
  id: string
  idempotencyKey: string
  fileName: string
  totalRows: number
  importedRows: number
  duplicateRows: number
  rejectedRows: number
  errors: { line: number; message: string }[]
  createdAt: string
}

export type ReconciliationRun = {
  id: string
  triggerSource: "MANUAL" | "SCHEDULED"
  businessDate: string
  ordersChecked: number
  reconciled: number
  divergent: number
  stillPending: number
  discrepanciesFound: number
  startedAt: string
  finishedAt: string | null
}

export type Discrepancy = {
  id: string
  type: DiscrepancyType
  status: DiscrepancyStatus
  orderExternalId: string | null
  payoutExternalId: string | null
  expectedAmount: number
  actualAmount: number
  difference: number
  description: string
  resolutionNote: string | null
  detectedAt: string
  resolvedAt: string | null
}

export type ReconciliationReport = {
  from: string
  to: string
  ordersByStatus: Record<ReconciliationStatus, number>
  discrepanciesByType: { type: DiscrepancyType; count: number; totalDifference: number }[]
  openDiscrepancies: number
}

export type StoreAlert = {
  id: string
  discrepancyId: string
  type: DiscrepancyType
  difference: number
  message: string
  createdAt: string
}

export type TokenResponse = { accessToken: string; tokenType: string; expiresIn: number }

export type ProblemDetail = {
  title?: string
  status?: number
  detail?: string
  errors?: { field: string; message: string }[]
}
