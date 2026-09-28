import type { ReactNode } from "react"

import { Label } from "@/components/ui/label"

export function Field({ id, label, error, hint, children }: { id: string; label: string; error?: string; hint?: ReactNode; children: ReactNode }) {
  return (
    <div className="grid gap-1.5">
      <Label htmlFor={id} className="text-[13px]">
        {label}
      </Label>
      {children}
      {error ? (
        <p className="text-xs text-loss" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-muted-foreground">{hint}</p>
      ) : null}
    </div>
  )
}
