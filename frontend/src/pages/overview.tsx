import { useMemo, useState } from "react"
import { Link, useParams } from "react-router"
import { ArrowRightIcon } from "@phosphor-icons/react"
import { endOfMonth, format, startOfMonth, subMonths } from "date-fns"
import { ptBR } from "date-fns/locale"

import { Empty, ErrorLine } from "@/components/empty"
import { TypeLabel } from "@/components/markers"
import { Amount, Diff } from "@/components/money"
import { PageHeader, SectionLabel } from "@/components/page-header"
import { RunButton } from "@/components/run-button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useAlerts, useDiscrepancies, useOrders, useReport, useRuns } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { DISCREPANCY_TYPES, impact } from "@/lib/discrepancy"
import { alertDetail, count, dateTime, dayShort, duration, money } from "@/lib/format"
import type { ReconciliationReport, ReconciliationRun } from "@/lib/types"
import { cn } from "@/lib/utils"

const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

function months() {
  const now = new Date()
  return Array.from({ length: 6 }, (_, i) => {
    const date = subMonths(now, i)
    return {
      value: format(date, "yyyy-MM"),
      label: capitalize(format(date, "MMMM 'de' yyyy", { locale: ptBR })),
      from: format(startOfMonth(date), "yyyy-MM-dd"),
      to: format(endOfMonth(date), "yyyy-MM-dd"),
    }
  })
}

function Figure({ label, value, detail, tone }: { label: string; value: string; detail?: string; tone?: "loss" | "ok" }) {
  return (
    <div className="grid content-start gap-1 px-5 py-4">
      <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground">{label}</span>
      <span className={cn("font-mono text-[28px] leading-none font-medium tabular", tone === "loss" && "text-loss", tone === "ok" && "text-ok")}>
        {value}
      </span>
      {detail && <span className="text-xs text-muted-foreground">{detail}</span>}
    </div>
  )
}

function Ledger({ report }: { report: ReconciliationReport }) {
  const { RECONCILED, DIVERGENT, PENDING } = report.ordersByStatus
  const total = RECONCILED + DIVERGENT + PENDING
  const pct = (n: number) => (total === 0 ? 0 : (n / total) * 100)
  const effects = report.discrepanciesByType.map((t) => impact(t.type, t.totalDifference))
  // Subtrai em vez de negar a soma: -(0) vira -0, que o Intl formata como "-R$ 0,00".
  const toRecover = effects.filter((v) => v < 0).reduce((a, b) => a - b, 0)
  const unexplained = effects.filter((v) => v > 0).reduce((a, b) => a + b, 0)

  return (
    <section className="border border-border bg-card" aria-label="Resumo do período">
      <div className="grid grid-cols-2 divide-border md:grid-cols-5 md:divide-x [&>*]:border-b [&>*]:border-border md:[&>*]:border-b-0">
        <Figure label="Pedidos" value={count(total)} detail="com data no período" />
        <Figure label="Conciliados" value={count(RECONCILED)} detail={`${pct(RECONCILED).toFixed(0)}% dos pedidos`} tone="ok" />
        <Figure label="Divergentes" value={count(DIVERGENT)} detail={`${pct(DIVERGENT).toFixed(0)}% dos pedidos`} tone={DIVERGENT > 0 ? "loss" : undefined} />
        <Figure label="No prazo" value={count(PENDING)} detail="aguardando o repasse" />
        <Figure
          label="A recuperar"
          value={money(toRecover)}
          detail={unexplained > 0 ? `+ ${money(unexplained)} recebidos sem explicação` : `${count(report.openDiscrepancies)} divergências abertas`}
          tone={toRecover > 0 ? "loss" : undefined}
        />
      </div>
      {/* Composição dos pedidos numa barra só */}
      <div className="flex h-1.5 w-full bg-muted" aria-hidden="true">
        <div className="bg-ok" style={{ width: `${pct(RECONCILED)}%` }} />
        <div className="bg-loss" style={{ width: `${pct(DIVERGENT)}%` }} />
        <div className="bg-pending/60" style={{ width: `${pct(PENDING)}%` }} />
      </div>
    </section>
  )
}

/** Barras divergentes em torno do zero: perda à esquerda, dinheiro sobrando à direita. */
function ImpactChart({ report }: { report: ReconciliationReport }) {
  const rows = DISCREPANCY_TYPES.map((type) => {
    const found = report.discrepanciesByType.find((t) => t.type === type)
    return { type, count: found?.count ?? 0, value: impact(type, found?.totalDifference ?? 0) }
  })
  const max = Math.max(...rows.map((r) => Math.abs(r.value)), 1)

  return (
    <div className="grid content-start gap-3">
      <SectionLabel aside={<span className="font-mono text-[11px] text-muted-foreground">efeito no caixa, R$</span>}>
        Impacto por tipo de divergência
      </SectionLabel>
      <div className="grid grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_minmax(0,1fr)_76px] items-center gap-y-2 text-sm sm:grid-cols-[180px_42px_minmax(0,1fr)_minmax(0,1fr)_96px]">
        <span />
        <span className="hidden text-right text-[11px] text-muted-foreground sm:block">qtd.</span>
        <span className="pr-2 text-right text-[11px] text-loss">perda</span>
        <span className="pl-2 text-[11px] text-surplus">sobra</span>
        <span />
        {rows.map((row) => {
          const width = `${(Math.abs(row.value) / max) * 100}%`
          return (
            <div key={row.type} className="contents">
              <span className={cn("truncate", row.count === 0 && "text-muted-foreground")}>
                <TypeLabel type={row.type} />
              </span>
              <span className="hidden text-right font-mono text-[13px] tabular text-muted-foreground sm:block">{row.count}</span>
              <div className="flex h-6 justify-end border-r border-foreground/70">
                {row.value < 0 && <div className="h-full bg-loss/85" style={{ width }} />}
              </div>
              <div className="flex h-6">
                {row.value > 0 && <div className="h-full bg-surplus/80" style={{ width }} />}
              </div>
              <Diff value={row.value} className="text-right" />
            </div>
          )
        })}
      </div>
      <p className="text-xs text-muted-foreground">
        Perda é o que a loja deve cobrar do adquirente. Sobra é dinheiro que entrou sem pedido ou em dobro e precisa ser estornado ou explicado.
      </p>
    </div>
  )
}

function Receipt({ run }: { run: ReconciliationRun }) {
  const lines: [string, string][] = [
    ["Pedidos verificados", count(run.ordersChecked)],
    ["Conciliados", count(run.reconciled)],
    ["Divergentes", count(run.divergent)],
    ["Ainda no prazo", count(run.stillPending)],
    ["Divergências novas", count(run.discrepanciesFound)],
    ["Duração", duration(run.startedAt, run.finishedAt)],
  ]
  return (
    <div className="border border-border bg-card font-mono text-[12.5px]">
      <div className="flex justify-between border-b border-dashed border-border px-4 py-2.5 text-[11px] uppercase tracking-wider text-muted-foreground">
        <span>{run.triggerSource === "MANUAL" ? "Manual" : "Agendada 03:00"}</span>
        <span className="tabular">{dateTime(run.startedAt)}</span>
      </div>
      <dl className="grid gap-1.5 px-4 py-3">
        {lines.map(([label, value]) => (
          <div key={label} className="flex items-baseline gap-2">
            <dt>{label}</dt>
            <span className="flex-1 translate-y-[-3px] border-b border-dotted border-muted-foreground/50" aria-hidden="true" />
            <dd className="tabular">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

export function OverviewPage() {
  const { storeId = "" } = useParams()
  const options = useMemo(() => months(), [])
  // Sem escolha do usuário, abre no mês do pedido mais recente (a lista vem por orderDate desc),
  // para a tela não abrir zerada quando o mês corrente ainda não tem pedidos.
  const latest = useOrders(storeId, { page: 0 })
  const latestMonth = latest.data?.content[0]?.orderDate.slice(0, 7)
  const [picked, setPicked] = useState<string>()
  const month = picked ?? options.find((o) => o.value === latestMonth)?.value ?? options[0].value
  const period = options.find((o) => o.value === month) ?? options[0]

  const report = useReport(storeId, period.from, period.to)
  const runs = useRuns(storeId, 0, 1)
  const open = useDiscrepancies(storeId, { status: "OPEN", page: 0, size: 6 })
  const alerts = useAlerts(storeId, 0, 4)
  const lastRun = runs.data?.content[0]

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Visão geral"
        description="Como os pedidos do período bateram com o que o adquirente repassou."
        actions={
          <>
            <Select value={month} onValueChange={setPicked}>
              <SelectTrigger className="w-[190px]" aria-label="Período">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {options.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <RunButton storeId={storeId} />
          </>
        }
      />

      {report.isError && <ErrorLine message={describeError(report.error).title} />}
      {report.data ? <Ledger report={report.data} /> : <Skeleton className="h-[118px]" />}

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
        {report.data ? <ImpactChart report={report.data} /> : <Skeleton className="h-64" />}

        <div className="grid content-start gap-6">
          <div className="grid gap-3">
            <SectionLabel
              aside={
                <Link to={`/lojas/${storeId}/execucoes`} className="text-xs text-primary hover:underline">
                  histórico
                </Link>
              }
            >
              Última execução
            </SectionLabel>
            {runs.isLoading ? (
              <Skeleton className="h-52" />
            ) : lastRun ? (
              <Receipt run={lastRun} />
            ) : (
              <Empty title="O motor ainda não rodou">Clique em Conciliar agora ou espere o job das 03:00.</Empty>
            )}
          </div>

          <div className="grid gap-3">
            <SectionLabel
              aside={
                <Link to={`/lojas/${storeId}/alertas`} className="text-xs text-primary hover:underline">
                  todos
                </Link>
              }
            >
              Alertas recentes
            </SectionLabel>
            {alerts.data && alerts.data.content.length > 0 ? (
              <ul className="grid">
                {alerts.data.content.map((alert) => (
                  <li key={alert.id} className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-0.5 border-b border-dashed border-border py-2 text-[13px] last:border-0">
                    <span className="row-span-2 font-mono text-[11px] text-muted-foreground tabular">{dayShort(alert.createdAt)}</span>
                    <TypeLabel type={alert.type} />
                    <span className="text-muted-foreground">{alertDetail(alert.message)}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">Nenhum alerta ainda. Eles chegam pelo Kafka logo depois de cada execução.</p>
            )}
          </div>
        </div>
      </div>

      <div className="grid gap-3">
        <SectionLabel
          aside={
            <Link to={`/lojas/${storeId}/divergencias`} className="inline-flex items-center gap-1 text-xs text-primary hover:underline">
              ver todas <ArrowRightIcon className="size-3" />
            </Link>
          }
        >
          Divergências abertas
        </SectionLabel>
        {open.data && open.data.content.length === 0 ? (
          <Empty title="Nada em aberto">Todas as divergências foram resolvidas.</Empty>
        ) : (
          <div className="overflow-x-auto border border-border bg-card">
            <Table className="ledger-rows">
              <TableHeader>
                <TableRow>
                  <TableHead>Divergência</TableHead>
                  <TableHead>Pedido</TableHead>
                  <TableHead>Repasse</TableHead>
                  <TableHead className="text-right">Esperado</TableHead>
                  <TableHead className="text-right">Recebido</TableHead>
                  <TableHead className="text-right">Diferença</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {open.data?.content.map((d) => (
                  <TableRow key={d.id}>
                    <TableCell>
                      <TypeLabel type={d.type} />
                    </TableCell>
                    <TableCell className="font-mono text-[13px]">{d.orderExternalId ?? "—"}</TableCell>
                    <TableCell className="font-mono text-[13px]">{d.payoutExternalId ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      <Amount value={d.expectedAmount} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Amount value={d.actualAmount} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Diff value={d.difference} type={d.type} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </div>
  )
}
