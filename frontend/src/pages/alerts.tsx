import { useState } from "react"
import { useParams } from "react-router"

import { Empty, ErrorLine } from "@/components/empty"
import { TypeLabel } from "@/components/markers"
import { Diff } from "@/components/money"
import { PageHeader } from "@/components/page-header"
import { Pager } from "@/components/pager"
import { Skeleton } from "@/components/ui/skeleton"
import { useAlerts } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { alertDetail, day, time } from "@/lib/format"
import type { StoreAlert } from "@/lib/types"

function groupByDay(alerts: StoreAlert[]) {
  const groups = new Map<string, StoreAlert[]>()
  for (const alert of alerts) {
    const key = day(alert.createdAt)
    groups.set(key, [...(groups.get(key) ?? []), alert])
  }
  return [...groups.entries()]
}

export function AlertsPage() {
  const { storeId = "" } = useParams()
  const [page, setPage] = useState(0)
  const query = useAlerts(storeId, page)

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Alertas"
        description={
          <>
            Cada divergência vira um evento no Kafka; um consumidor idempotente gera um alerta por evento.{" "}
            <span className="inline-flex items-center gap-1.5 whitespace-nowrap">
              <span className="size-1.5 animate-pulse rounded-full bg-ok" aria-hidden="true" /> atualiza a cada 5 s
            </span>
          </>
        }
      />
      {query.isError && <ErrorLine message={describeError(query.error).title} />}
      {query.isLoading ? (
        <Skeleton className="h-80" />
      ) : query.data?.content.length === 0 ? (
        <Empty title="Nenhum alerta">Rode a conciliação: os alertas aparecem aqui segundos depois, quando o evento passa pelo Kafka.</Empty>
      ) : (
        <div className="grid gap-6">
          {groupByDay(query.data?.content ?? []).map(([label, alerts]) => (
            <section key={label} className="grid gap-1">
              <h2 className="border-b border-foreground/80 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.12em] text-muted-foreground">
                {label}
              </h2>
              <ul>
                {alerts.map((alert) => (
                  <li
                    key={alert.id}
                    className="grid grid-cols-[64px_minmax(0,1fr)_auto] items-baseline gap-x-4 gap-y-1 border-b border-dashed border-border py-3 text-sm sm:grid-cols-[64px_170px_minmax(0,1fr)_auto]"
                  >
                    <span className="font-mono text-[12px] text-muted-foreground tabular">{time(alert.createdAt)}</span>
                    <span className="hidden sm:block">
                      <TypeLabel type={alert.type} />
                    </span>
                    <span>{alertDetail(alert.message)}</span>
                    <Diff value={alert.difference} type={alert.type} className="text-right" />
                  </li>
                ))}
              </ul>
            </section>
          ))}
          <Pager page={query.data?.page} noun="alertas" onChange={setPage} />
        </div>
      )}
    </div>
  )
}
