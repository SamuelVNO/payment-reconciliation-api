import { impact } from "@/lib/discrepancy"
import { amount, signed } from "@/lib/format"
import type { DiscrepancyType } from "@/lib/types"
import { cn } from "@/lib/utils"

export function Amount({ value, className }: { value: number; className?: string }) {
  return <span className={cn("font-mono tabular text-[13px]", className)}>{amount(value)}</span>
}

/**
 * Diferença recebido − esperado. A cor segue o impacto para a loja (vermelho = perdeu),
 * que em "taxa acima" tem o sinal invertido.
 */
export function Diff({ value, type, className }: { value: number; type?: DiscrepancyType; className?: string }) {
  const effect = type ? impact(type, value) : value
  return (
    <span
      className={cn(
        "font-mono tabular text-[13px] font-medium",
        effect < 0 && "text-loss",
        effect > 0 && "text-surplus",
        effect === 0 && "text-muted-foreground",
        className,
      )}
    >
      {signed(value)}
    </span>
  )
}
