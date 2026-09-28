import type { ReactNode } from "react"

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="grid place-items-center gap-1 border border-dashed border-border px-6 py-12 text-center">
      <p className="text-sm font-medium">{title}</p>
      {children && <div className="max-w-md text-sm text-muted-foreground">{children}</div>}
    </div>
  )
}

export function ErrorLine({ message }: { message: string }) {
  return <p className="border border-loss/30 bg-loss/5 px-3 py-2 text-sm text-loss">{message}</p>
}
