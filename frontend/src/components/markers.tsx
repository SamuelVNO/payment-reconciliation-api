import { DISCREPANCY, ORDER_STATUS } from "@/lib/discrepancy"
import type { DiscrepancyStatus, DiscrepancyType, ReconciliationStatus } from "@/lib/types"
import { cn } from "@/lib/utils"

const toneBg = {
  loss: "bg-loss",
  surplus: "bg-surplus",
  ok: "bg-ok",
  pending: "bg-pending",
} as const

export function TypeLabel({ type, withCode = false }: { type: DiscrepancyType; withCode?: boolean }) {
  const info = DISCREPANCY[type]
  return (
    <span className="inline-flex items-center gap-2">
      <span className={cn("size-2 shrink-0", toneBg[info.tone])} aria-hidden="true" />
      <span className="font-medium">{info.label}</span>
      {withCode && <span className="font-mono text-[11px] text-muted-foreground">{type}</span>}
    </span>
  )
}

export function OrderStatusLabel({ status }: { status: ReconciliationStatus }) {
  const info = ORDER_STATUS[status]
  return (
    <span className="inline-flex items-center gap-2 text-[13px]">
      <span className={cn("size-1.5 rounded-full", toneBg[info.tone])} aria-hidden="true" />
      {info.label}
    </span>
  )
}

export function DiscrepancyStatusLabel({ status }: { status: DiscrepancyStatus }) {
  return status === "OPEN" ? (
    <span className="border border-border px-1.5 py-0.5 font-mono text-[11px] tracking-wide">ABERTA</span>
  ) : (
    <span className="border border-ok/40 px-1.5 py-0.5 font-mono text-[11px] tracking-wide text-ok">RESOLVIDA</span>
  )
}
