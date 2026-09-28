import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { request, requestWithHeaders } from "@/lib/api"
import type {
  Discrepancy,
  DiscrepancyStatus,
  DiscrepancyType,
  Order,
  Page,
  Payout,
  PayoutImport,
  ReconciliationReport,
  ReconciliationRun,
  ReconciliationStatus,
  Store,
  StoreAlert,
} from "@/lib/types"

const base = (storeId: string) => `/api/stores/${storeId}`

// Chaves hierárquicas: invalidar ["store", id] recarrega tudo daquela loja de uma vez
export const keys = {
  stores: ["stores"] as const,
  store: (id: string) => ["store", id] as const,
  report: (id: string, from: string, to: string) => ["store", id, "report", from, to] as const,
  discrepancies: (id: string, params: object) => ["store", id, "discrepancies", params] as const,
  orders: (id: string, params: object) => ["store", id, "orders", params] as const,
  payouts: (id: string, page: number) => ["store", id, "payouts", page] as const,
  imports: (id: string, page: number) => ["store", id, "imports", page] as const,
  runs: (id: string, page: number) => ["store", id, "runs", page] as const,
  alerts: (id: string, page: number) => ["store", id, "alerts", page] as const,
}

export function useStores(page = 0) {
  return useQuery({
    queryKey: [...keys.stores, page],
    queryFn: () => request<Page<Store>>("/api/stores", { query: { page, size: 50, sort: "name" } }),
  })
}

export function useStore(storeId: string) {
  return useQuery({ queryKey: keys.store(storeId), queryFn: () => request<Store>(base(storeId)) })
}

export function useReport(storeId: string, from: string, to: string) {
  return useQuery({
    queryKey: keys.report(storeId, from, to),
    queryFn: () => request<ReconciliationReport>(`${base(storeId)}/reconciliation-report`, { query: { from, to } }),
  })
}

export function useDiscrepancies(
  storeId: string,
  params: { type?: DiscrepancyType; status?: DiscrepancyStatus; page: number; size?: number },
) {
  return useQuery({
    queryKey: keys.discrepancies(storeId, params),
    queryFn: () =>
      request<Page<Discrepancy>>(`${base(storeId)}/discrepancies`, {
        query: { type: params.type, status: params.status, page: params.page, size: params.size ?? 15 },
      }),
    placeholderData: keepPreviousData,
    enabled: storeId !== "",
  })
}

export function useOrders(storeId: string, params: { status?: ReconciliationStatus; page: number }) {
  return useQuery({
    queryKey: keys.orders(storeId, params),
    queryFn: () =>
      request<Page<Order>>(`${base(storeId)}/orders`, { query: { status: params.status, page: params.page, size: 15 } }),
    placeholderData: keepPreviousData,
  })
}

export function usePayouts(storeId: string, page: number) {
  return useQuery({
    queryKey: keys.payouts(storeId, page),
    queryFn: () => request<Page<Payout>>(`${base(storeId)}/payouts`, { query: { page, size: 15 } }),
    placeholderData: keepPreviousData,
  })
}

export function useImports(storeId: string, page: number) {
  return useQuery({
    queryKey: keys.imports(storeId, page),
    queryFn: () => request<Page<PayoutImport>>(`${base(storeId)}/payout-imports`, { query: { page, size: 10 } }),
    placeholderData: keepPreviousData,
  })
}

export function useRuns(storeId: string, page: number, size = 15) {
  return useQuery({
    queryKey: keys.runs(storeId, page),
    queryFn: () => request<Page<ReconciliationRun>>(`${base(storeId)}/reconciliation-runs`, { query: { page, size } }),
    placeholderData: keepPreviousData,
  })
}

export function useAlerts(storeId: string, page: number, size = 20) {
  return useQuery({
    queryKey: keys.alerts(storeId, page),
    queryFn: () => request<Page<StoreAlert>>(`${base(storeId)}/alerts`, { query: { page, size } }),
    placeholderData: keepPreviousData,
    // Os alertas chegam pelo Kafka alguns segundos depois da conciliação
    refetchInterval: 5_000,
  })
}

function useInvalidateStore(storeId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: keys.store(storeId) })
}

export function useRunReconciliation(storeId: string) {
  const invalidate = useInvalidateStore(storeId)
  return useMutation({
    mutationFn: () => request<ReconciliationRun>(`${base(storeId)}/reconciliation-runs`, { method: "POST" }),
    onSuccess: invalidate,
  })
}

export function useResolveDiscrepancy(storeId: string) {
  const invalidate = useInvalidateStore(storeId)
  return useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) =>
      request<void>(`${base(storeId)}/discrepancies/${id}/resolve`, { method: "PATCH", body: { note } }),
    onSuccess: invalidate,
  })
}

export function useCreateOrder(storeId: string) {
  const invalidate = useInvalidateStore(storeId)
  return useMutation({
    mutationFn: (body: { externalId: string; amount: number; orderDate: string }) =>
      request<Order>(`${base(storeId)}/orders`, { method: "POST", body }),
    onSuccess: invalidate,
  })
}

export function useImportPayouts(storeId: string) {
  const invalidate = useInvalidateStore(storeId)
  return useMutation({
    mutationFn: async ({ file, idempotencyKey }: { file: File; idempotencyKey: string }) => {
      const form = new FormData()
      form.append("file", file)
      const { data, headers } = await requestWithHeaders<PayoutImport>(`${base(storeId)}/payout-imports`, {
        method: "POST",
        body: form,
        headers: { "Idempotency-Key": idempotencyKey },
      })
      return { result: data, replayed: headers.get("Idempotent-Replayed") === "true" }
    },
    onSuccess: invalidate,
  })
}

export function useCreateStore() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { name: string; document: string; feeRate: number }) =>
      request<Store>("/api/stores", { method: "POST", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.stores }),
  })
}

export function useCreateStoreUser() {
  return useMutation({
    mutationFn: (body: { email: string; password: string; storeId: string }) =>
      request<{ id: string; email: string }>("/api/users", { method: "POST", body }),
  })
}
