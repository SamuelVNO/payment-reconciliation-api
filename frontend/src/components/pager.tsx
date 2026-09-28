import { CaretLeftIcon, CaretRightIcon } from "@phosphor-icons/react"

import { Button } from "@/components/ui/button"
import { count } from "@/lib/format"
import type { Page } from "@/lib/types"

export function Pager({ page, onChange, noun = "itens" }: { page?: Page<unknown>["page"]; onChange: (p: number) => void; noun?: string }) {
  if (!page || page.totalElements === 0) return null
  const last = Math.max(page.totalPages - 1, 0)
  return (
    <div className="flex items-center justify-between gap-3 pt-3 text-xs text-muted-foreground">
      <span className="tabular">
        {count(page.totalElements)} {noun} · página {page.number + 1} de {Math.max(page.totalPages, 1)}
      </span>
      <div className="flex gap-1">
        <Button variant="outline" size="icon-sm" aria-label="Página anterior" disabled={page.number === 0} onClick={() => onChange(page.number - 1)}>
          <CaretLeftIcon />
        </Button>
        <Button variant="outline" size="icon-sm" aria-label="Próxima página" disabled={page.number >= last} onClick={() => onChange(page.number + 1)}>
          <CaretRightIcon />
        </Button>
      </div>
    </div>
  )
}
