import { ArrowsClockwiseIcon } from "@phosphor-icons/react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { useRunReconciliation } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { count } from "@/lib/format"

export function RunButton({ storeId }: { storeId: string }) {
  const run = useRunReconciliation(storeId)
  return (
    <Button
      disabled={run.isPending}
      onClick={() =>
        run.mutate(undefined, {
          onSuccess: (r) => {
            const found = r.discrepanciesFound
            toast.success(`${count(r.ordersChecked)} pedidos verificados`, {
              description:
                `${count(r.reconciled)} conciliados · ${count(r.divergent)} divergentes · ${count(r.stillPending)} no prazo. ` +
                (found > 0 ? `${count(found)} divergências novas.` : "Nenhuma divergência nova."),
            })
          },
          onError: (error) => toast.error(describeError(error).title, { description: describeError(error).description }),
        })
      }
    >
      <ArrowsClockwiseIcon className={run.isPending ? "animate-spin" : undefined} />
      {run.isPending ? "Conciliando…" : "Conciliar agora"}
    </Button>
  )
}
