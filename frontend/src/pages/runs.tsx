import { useState } from "react"
import { useParams } from "react-router"

import { Empty, ErrorLine } from "@/components/empty"
import { PageHeader } from "@/components/page-header"
import { Pager } from "@/components/pager"
import { RunButton } from "@/components/run-button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useRuns } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { count, dateTime, duration } from "@/lib/format"
import { cn } from "@/lib/utils"

export function RunsPage() {
  const { storeId = "" } = useParams()
  const [page, setPage] = useState(0)
  const query = useRuns(storeId, page)

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Execuções"
        description="Cada vez que o motor rodou: manualmente ou pelo job das 03:00. Rodar de novo nunca duplica divergência."
        actions={<RunButton storeId={storeId} />}
      />
      {query.isError && <ErrorLine message={describeError(query.error).title} />}
      {query.isLoading ? (
        <Skeleton className="h-80" />
      ) : query.data?.content.length === 0 ? (
        <Empty title="O motor ainda não rodou para esta loja" />
      ) : (
        <div>
          <div className="overflow-x-auto border border-border bg-card">
            <Table className="ledger-rows">
              <TableHeader>
                <TableRow>
                  <TableHead>Início</TableHead>
                  <TableHead>Origem</TableHead>
                  <TableHead className="text-right">Verificados</TableHead>
                  <TableHead className="text-right">Conciliados</TableHead>
                  <TableHead className="text-right">Divergentes</TableHead>
                  <TableHead className="text-right">No prazo</TableHead>
                  <TableHead className="text-right">Divergências novas</TableHead>
                  <TableHead className="text-right">Duração</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody className={query.isPlaceholderData ? "opacity-60" : undefined}>
                {query.data?.content.map((run) => (
                  <TableRow key={run.id}>
                    <TableCell className="font-mono text-[13px] tabular">{dateTime(run.startedAt)}</TableCell>
                    <TableCell>
                      <span className="border border-border px-1.5 py-0.5 font-mono text-[11px] tracking-wide">
                        {run.triggerSource === "MANUAL" ? "MANUAL" : "AGENDADA"}
                      </span>
                    </TableCell>
                    <TableCell className="text-right font-mono tabular">{count(run.ordersChecked)}</TableCell>
                    <TableCell className="text-right font-mono tabular text-ok">{count(run.reconciled)}</TableCell>
                    <TableCell className={cn("text-right font-mono tabular", run.divergent > 0 && "text-loss")}>{count(run.divergent)}</TableCell>
                    <TableCell className="text-right font-mono tabular text-muted-foreground">{count(run.stillPending)}</TableCell>
                    <TableCell className="text-right font-mono tabular font-medium">{count(run.discrepanciesFound)}</TableCell>
                    <TableCell className="text-right font-mono text-[12px] tabular text-muted-foreground">
                      {duration(run.startedAt, run.finishedAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Pager page={query.data?.page} noun="execuções" onChange={setPage} />
        </div>
      )}
    </div>
  )
}
