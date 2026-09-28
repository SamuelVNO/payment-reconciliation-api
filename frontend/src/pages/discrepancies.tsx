import { useState, type ReactNode } from "react"
import { useParams, useSearchParams } from "react-router"
import { toast } from "sonner"

import { Empty, ErrorLine } from "@/components/empty"
import { DiscrepancyStatusLabel, TypeLabel } from "@/components/markers"
import { Amount, Diff } from "@/components/money"
import { PageHeader } from "@/components/page-header"
import { Pager } from "@/components/pager"
import { RunButton } from "@/components/run-button"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { useDiscrepancies, useResolveDiscrepancy } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { DISCREPANCY, DISCREPANCY_TYPES } from "@/lib/discrepancy"
import { dateTime, dayShort } from "@/lib/format"
import type { Discrepancy, DiscrepancyStatus, DiscrepancyType } from "@/lib/types"

const ALL = "ALL"

function Detail({ storeId, discrepancy, onClose }: { storeId: string; discrepancy: Discrepancy | null; onClose: () => void }) {
  const resolve = useResolveDiscrepancy(storeId)
  const [note, setNote] = useState("")
  const d = discrepancy

  const rows: { label: string; value: ReactNode }[] = d
    ? [
        { label: "Pedido", value: <span className="font-mono">{d.orderExternalId ?? "—"}</span> },
        { label: "Repasse", value: <span className="font-mono">{d.payoutExternalId ?? "—"}</span> },
        { label: d.type === "FEE_OVERCHARGED" ? "Taxa esperada" : "Esperado", value: <Amount value={d.expectedAmount} /> },
        { label: d.type === "FEE_OVERCHARGED" ? "Taxa cobrada" : "Recebido", value: <Amount value={d.actualAmount} /> },
        { label: "Diferença", value: <Diff value={d.difference} type={d.type} /> },
        { label: "Detectada em", value: <span className="tabular">{dateTime(d.detectedAt)}</span> },
        { label: "Status", value: <DiscrepancyStatusLabel status={d.status} /> },
      ]
    : []

  return (
    <Sheet open={d !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="w-full gap-0 sm:max-w-md">
        {d && (
          <>
            <SheetHeader className="border-b border-border">
              <SheetTitle className="flex items-center gap-2 text-lg">
                <TypeLabel type={d.type} />
              </SheetTitle>
              <SheetDescription>{DISCREPANCY[d.type].explain}.</SheetDescription>
            </SheetHeader>
            <div className="grid gap-6 overflow-y-auto p-4">
              <dl className="grid text-sm">
                {rows.map(({ label, value }) => (
                  <div key={label} className="flex items-center justify-between gap-4 border-b border-dashed border-border py-2">
                    <dt className="text-muted-foreground">{label}</dt>
                    <dd>{value}</dd>
                  </div>
                ))}
              </dl>

              <div className="grid gap-1 text-xs">
                <span className="font-semibold uppercase tracking-[0.1em] text-muted-foreground">Mensagem do motor</span>
                <code className="bg-muted px-2 py-1.5 font-mono text-[12px]">{d.description}</code>
              </div>

              {d.status === "RESOLVED" ? (
                <div className="grid gap-1 border border-ok/30 bg-ok/5 p-3 text-sm">
                  <span className="font-medium text-ok">Resolvida em {d.resolvedAt ? dateTime(d.resolvedAt) : "—"}</span>
                  <span>{d.resolutionNote}</span>
                </div>
              ) : (
                <form
                  className="grid gap-2"
                  onSubmit={(event) => {
                    event.preventDefault()
                    resolve.mutate(
                      { id: d.id, note: note.trim() },
                      {
                        onSuccess: () => {
                          toast.success("Divergência resolvida")
                          setNote("")
                          onClose()
                        },
                        onError: (error) => toast.error(describeError(error).title, { description: describeError(error).description }),
                      },
                    )
                  }}
                >
                  <label htmlFor="resolution-note" className="text-sm font-medium">
                    Como foi resolvida
                  </label>
                  <Textarea
                    id="resolution-note"
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Ex.: adquirente estornou a diferença no repasse de 02/10"
                    maxLength={255}
                    rows={3}
                  />
                  <Button type="submit" className="justify-self-start" disabled={note.trim().length < 3 || resolve.isPending}>
                    {resolve.isPending ? "Salvando…" : "Marcar como resolvida"}
                  </Button>
                </form>
              )}
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}

export function DiscrepanciesPage() {
  const { storeId = "" } = useParams()
  const [params, setParams] = useSearchParams()
  const status = (params.get("status") ?? "OPEN") as DiscrepancyStatus | typeof ALL
  const type = (params.get("tipo") ?? ALL) as DiscrepancyType | typeof ALL
  const page = Number(params.get("pagina") ?? 0)
  const [selected, setSelected] = useState<Discrepancy | null>(null)

  const query = useDiscrepancies(storeId, {
    status: status === ALL ? undefined : status,
    type: type === ALL ? undefined : type,
    page,
  })

  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(changes)) {
      if (value === null) next.delete(key)
      else next.set(key, value)
    }
    if (!("pagina" in changes)) next.delete("pagina")
    setParams(next, { replace: true })
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Divergências"
        description="Tudo o que o motor encontrou fora do lugar. Clique numa linha para ver o detalhe e registrar a resolução."
        actions={<RunButton storeId={storeId} />}
      />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Tabs value={status} onValueChange={(v) => update({ status: v })}>
          <TabsList>
            <TabsTrigger value="OPEN">Abertas</TabsTrigger>
            <TabsTrigger value="RESOLVED">Resolvidas</TabsTrigger>
            <TabsTrigger value={ALL}>Todas</TabsTrigger>
          </TabsList>
        </Tabs>
        <Select value={type} onValueChange={(v) => update({ tipo: v === ALL ? null : v })}>
          <SelectTrigger className="w-[200px]" aria-label="Tipo de divergência">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Todos os tipos</SelectItem>
            {DISCREPANCY_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {DISCREPANCY[t].label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {query.isError && <ErrorLine message={describeError(query.error).title} />}

      {query.isLoading ? (
        <Skeleton className="h-80" />
      ) : query.data && query.data.content.length === 0 ? (
        <Empty title={status === "OPEN" ? "Nenhuma divergência aberta" : "Nenhuma divergência com esses filtros"}>
          {status === "OPEN" && "Importe repasses e rode a conciliação para o motor comparar com os pedidos."}
        </Empty>
      ) : (
        <div>
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
                  <TableHead>Detectada</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody className={query.isPlaceholderData ? "opacity-60" : undefined}>
                {query.data?.content.map((d) => (
                  <TableRow
                    key={d.id}
                    className="cursor-pointer"
                    tabIndex={0}
                    onClick={() => setSelected(d)}
                    onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && setSelected(d)}
                  >
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
                    <TableCell className="font-mono text-[12px] text-muted-foreground tabular">{dayShort(d.detectedAt)}</TableCell>
                    <TableCell>
                      <DiscrepancyStatusLabel status={d.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Pager page={query.data?.page} noun="divergências" onChange={(p) => update({ pagina: String(p) })} />
        </div>
      )}

      <Detail storeId={storeId} discrepancy={selected} onClose={() => setSelected(null)} />
    </div>
  )
}
