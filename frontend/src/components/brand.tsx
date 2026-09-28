import { cn } from "@/lib/utils"

/** Marca: duas colunas de razão contábil, uma batendo com a outra. */
export function BrandMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={cn("size-6", className)}>
      <rect x="2.5" y="3" width="8" height="18" rx="0.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <rect x="13.5" y="3" width="8" height="18" rx="0.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <path d="M5 8h3M5 12h3M5 16h3M16 8h3M16 12h3" stroke="currentColor" strokeWidth="1.5" />
      <path d="M15.5 15.5l1.5 1.5 3-3" stroke="var(--sidebar-primary, currentColor)" strokeWidth="1.6" fill="none" />
    </svg>
  )
}

export function Brand({ className }: { className?: string }) {
  return (
    <div className={cn("flex items-center gap-2.5", className)}>
      <BrandMark />
      <span className="font-heading text-[15px] font-semibold tracking-tight">Conciliador</span>
    </div>
  )
}
