import { useRef, useState } from "react"
import { useParams } from "react-router"
import { ArrowCounterClockwiseIcon, FileCsvIcon, UploadSimpleIcon, XIcon } from "@phosphor-icons/react"
import { toast } from "sonner"

import { Empty, ErrorLine } from "@/components/empty"
import { Amount } from "@/components/money"
import { PageHeader, SectionLabel } from "@/components/page-header"
import { Pager } from "@/components/pager"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useImportPayouts, useImports, usePayouts } from "@/hooks/api"
import { describeError } from "@/lib/api"
import { count, dateTime, day } from "@/lib/format"
import type { PayoutImport } from "@/lib/types"
import { cn } from "@/lib/utils"

const HEADER = "payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at"

function kb(bytes: number) {
  return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1).replace(".", ",")} KB`
}

function ImportResult({ result, replayed }: { result: PayoutImport; replayed: boolean }) {
  const figures: [string, number, string?][] = [
    ["Linhas", result.totalRows],
    ["Importadas", result.importedRows, "text-ok"],
    ["Duplicadas", result.duplicateRows, result.duplicateRows > 0 ? "text-surplus" : undefined],
    ["Rejeitadas", result.rejectedRows, result.rejectedRows > 0 ? "text-loss" : undefined],
  ]
  return (
    <div className="grid gap-4 border border-border bg-card p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="font-mono text-[13px]">{result.fileName}</span>
        {replayed ? (
          <span className="border border-primary/40 bg-accent px-2 py-0.5 text-xs text-accent-foreground">
            Mesma Idempotency-Key: a API devolveu o resultado original sem reprocessar
          </span>
        ) : (
          <span className="text-xs text-muted-foreground">{dateTime(result.createdAt)}</span>
        )}
      </div>
      <dl className="grid grid-cols-4 divide-x divide-border border-y border-border">
        {figures.map(([label, value, tone]) => (
          <div key={label} className="grid gap-0.5 px-3 py-2">
            <dt className="text-[11px] uppercase tracking-wider text-muted-foreground">{label}</dt>
            <dd className={cn("font-mono text-xl tabular", tone)}>{count(value)}</dd>
          </div>
        ))}
      </dl>
      {result.errors.length > 0 && (
        <div className="grid gap-2">
          <p className="text-sm font-medium">Linhas rejeitadas (o resto do arquivo foi importado)</p>
          <ul className="grid gap-1 font-mono text-[12.5px]">
            {result.errors.map((e) => (
              <li key={e.line} className="flex gap-3 bg-loss/5 px-2 py-1">
                <span className="text-loss tabular">linha {e.line}</span>
                <span>{e.message}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function Uploader({ storeId }: { storeId: string }) {
  const input = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [key, setKey] = useState(() => crypto.randomUUID())
  const [dragging, setDragging] = useState(false)
  const upload = useImportPayouts(storeId)

  const pick = (next: File | undefined) => {
    if (!next) return
    if (!next.name.toLowerCase().endsWith(".csv")) {
      toast.error("Envie um arquivo .csv")
      return
    }
    setFile(next)
    setKey(crypto.randomUUID())
    upload.reset()
  }

  const send = () =>
    file &&
    upload.mutate(
      { file, idempotencyKey: key },
      {
        onSuccess: ({ result, replayed }) =>
          replayed
            ? toast.info("Importação já feita com esta chave", { description: "Nada foi importado de novo." })
            : result.importedRows > 0
              ? toast.success(`${count(result.importedRows)} repasses importados`)
              : toast.info("Nenhum repasse novo", {
                  description: `${count(result.duplicateRows)} já estavam importados e ${count(result.rejectedRows)} foram rejeitados.`,
                }),
        onError: (error) => toast.error(describeError(error).title, { description: describeError(error).description }),
      },
    )

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <div className="grid content-start gap-3">
        <button
          type="button"
          onClick={() => input.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            pick(e.dataTransfer.files[0])
          }}
          className={cn(
            "grid place-items-center gap-2 border border-dashed border-input bg-card px-6 py-8 text-center transition-colors",
            "hover:border-primary hover:bg-accent/40",
            dragging && "border-primary bg-accent/60",
          )}
        >
          <FileCsvIcon className="size-8 text-primary" weight="duotone" />
          <span className="text-sm font-medium">Arraste o CSV do adquirente ou clique para escolher</span>
          <code className="max-w-full overflow-x-auto font-mono text-[11px] text-muted-foreground">{HEADER}</code>
        </button>
        <input
          ref={input}
          type="file"
          accept=".csv,text/csv"
          className="sr-only"
          aria-label="Arquivo CSV de repasses"
          onChange={(e) => {
            pick(e.target.files?.[0])
            e.target.value = ""
          }}
        />

        {file && (
          <div className="grid gap-3 border border-border bg-card p-4">
            <div className="flex items-center justify-between gap-3">
              <div className="grid">
                <span className="font-mono text-[13px]">{file.name}</span>
                <span className="text-xs text-muted-foreground">{kb(file.size)}</span>
              </div>
              <Button variant="ghost" size="icon-sm" aria-label="Remover arquivo" onClick={() => setFile(null)}>
                <XIcon />
              </Button>
            </div>
            <div className="grid gap-1">
              <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground">Idempotency-Key</span>
              <div className="flex items-center gap-2">
                <code className="flex-1 truncate bg-muted px-2 py-1 font-mono text-[12px]">{key}</code>
                <Button variant="outline" size="sm" onClick={() => setKey(crypto.randomUUID())}>
                  Nova chave
                </Button>
              </div>
              <span className="text-xs text-muted-foreground">
                Enviar de novo com a mesma chave devolve o resultado original. Com chave nova, repasses já importados contam como duplicados.
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button onClick={send} disabled={upload.isPending}>
                <UploadSimpleIcon /> {upload.isPending ? "Importando…" : "Importar"}
              </Button>
              {upload.isSuccess && (
                <Button variant="outline" onClick={send} disabled={upload.isPending}>
                  <ArrowCounterClockwiseIcon /> Reenviar com a mesma chave
                </Button>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="grid content-start gap-3">
        {upload.data ? (
          <ImportResult result={upload.data.result} replayed={upload.data.replayed} />
        ) : (
          <div className="grid gap-2 border border-dashed border-border p-4 text-sm text-muted-foreground">
            <p className="font-medium text-foreground">Como a importação funciona</p>
            <p>Cada linha é validada sozinha: valor com no máximo 2 casas, líquido igual a bruto menos taxa, data válida. Linha ruim é rejeitada e o resto entra.</p>
            <p>Um <span className="font-mono text-[12px]">payout_id</span> que já existe, neste ou em outro arquivo, nunca é importado duas vezes.</p>
            <p>O adquirente também pode depositar arquivos na pasta <span className="font-mono text-[12px]">payout-inbox/{"{storeId}"}</span>: um job importa a cada 5 minutos.</p>
          </div>
        )}
      </div>
    </div>
  )
}

function PayoutsTable({ storeId }: { storeId: string }) {
  const [page, setPage] = useState(0)
  const query = usePayouts(storeId, page)
  if (query.isLoading) return <Skeleton className="h-64" />
  if (query.isError) return <ErrorLine message={describeError(query.error).title} />
  if (query.data?.content.length === 0) return <Empty title="Nenhum repasse importado">Importe o CSV do adquirente acima.</Empty>
  return (
    <div>
      <div className="overflow-x-auto border border-border bg-card">
        <Table className="ledger-rows">
          <TableHeader>
            <TableRow>
              <TableHead>Repasse</TableHead>
              <TableHead>Pedido</TableHead>
              <TableHead className="text-right">Bruto</TableHead>
              <TableHead className="text-right">Taxa</TableHead>
              <TableHead className="text-right">Líquido</TableHead>
              <TableHead>Pago em</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody className={query.isPlaceholderData ? "opacity-60" : undefined}>
            {query.data?.content.map((p) => (
              <TableRow key={p.id}>
                <TableCell className="font-mono text-[13px] font-medium">{p.externalId}</TableCell>
                <TableCell className="font-mono text-[13px]">{p.orderExternalId}</TableCell>
                <TableCell className="text-right">
                  <Amount value={p.grossAmount} />
                </TableCell>
                <TableCell className="text-right">
                  <Amount value={p.feeAmount} className="text-muted-foreground" />
                </TableCell>
                <TableCell className="text-right">
                  <Amount value={p.netAmount} className="font-medium" />
                </TableCell>
                <TableCell>{day(p.paidAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      <Pager page={query.data?.page} noun="repasses" onChange={setPage} />
    </div>
  )
}

function ImportsTable({ storeId }: { storeId: string }) {
  const [page, setPage] = useState(0)
  const query = useImports(storeId, page)
  if (query.isLoading) return <Skeleton className="h-64" />
  if (query.isError) return <ErrorLine message={describeError(query.error).title} />
  if (query.data?.content.length === 0) return <Empty title="Nenhuma importação ainda" />
  return (
    <div>
      <div className="overflow-x-auto border border-border bg-card">
        <Table className="ledger-rows">
          <TableHeader>
            <TableRow>
              <TableHead>Arquivo</TableHead>
              <TableHead>Chave</TableHead>
              <TableHead className="text-right">Linhas</TableHead>
              <TableHead className="text-right">Importadas</TableHead>
              <TableHead className="text-right">Duplicadas</TableHead>
              <TableHead className="text-right">Rejeitadas</TableHead>
              <TableHead>Quando</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {query.data?.content.map((imp) => (
              <TableRow key={imp.id}>
                <TableCell className="font-mono text-[13px]">{imp.fileName}</TableCell>
                <TableCell className="max-w-[180px] truncate font-mono text-[12px] text-muted-foreground">{imp.idempotencyKey}</TableCell>
                <TableCell className="text-right font-mono tabular">{imp.totalRows}</TableCell>
                <TableCell className="text-right font-mono tabular text-ok">{imp.importedRows}</TableCell>
                <TableCell className="text-right font-mono tabular">{imp.duplicateRows}</TableCell>
                <TableCell className={cn("text-right font-mono tabular", imp.rejectedRows > 0 && "text-loss")}>{imp.rejectedRows}</TableCell>
                <TableCell className="font-mono text-[12px] text-muted-foreground tabular">{dateTime(imp.createdAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      <Pager page={query.data?.page} noun="importações" onChange={setPage} />
    </div>
  )
}

export function PayoutsPage() {
  const { storeId = "" } = useParams()
  return (
    <div className="grid gap-8">
      <PageHeader title="Repasses" description="O que o adquirente pagou de fato, importado do arquivo que ele envia." />
      <Uploader storeId={storeId} />
      <div className="grid gap-3">
        <SectionLabel>Histórico</SectionLabel>
        <Tabs defaultValue="payouts">
          <TabsList>
            <TabsTrigger value="payouts">Repasses importados</TabsTrigger>
            <TabsTrigger value="imports">Importações</TabsTrigger>
          </TabsList>
          <TabsContent value="payouts" className="pt-3">
            <PayoutsTable storeId={storeId} />
          </TabsContent>
          <TabsContent value="imports" className="pt-3">
            <ImportsTable storeId={storeId} />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
