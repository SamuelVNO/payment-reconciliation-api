import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useParams, useSearchParams } from "react-router"
import { PlusIcon } from "@phosphor-icons/react"
import { format } from "date-fns"
import { toast } from "sonner"
import { z } from "zod"

import { Empty, ErrorLine } from "@/components/empty"
import { Field } from "@/components/field"
import { OrderStatusLabel } from "@/components/markers"
import { Amount } from "@/components/money"
import { PageHeader } from "@/components/page-header"
import { Pager } from "@/components/pager"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useCreateOrder, useOrders } from "@/hooks/api"
import { ApiError, describeError } from "@/lib/api"
import { dateTime, day, parseAmount } from "@/lib/format"
import type { ReconciliationStatus } from "@/lib/types"

const ALL = "ALL"
const today = () => format(new Date(), "yyyy-MM-dd")

const schema = z.object({
  externalId: z.string().trim().min(1, "Informe o código do pedido").max(64),
  amount: z
    .string()
    .transform((v) => parseAmount(v))
    .pipe(z.number({ error: "Use um valor como 150,00" }).positive("O valor precisa ser maior que zero")),
  orderDate: z.string().refine((v) => v !== "" && v <= today(), "A data não pode estar no futuro"),
})
type Input_ = z.input<typeof schema>

function NewOrderDialog({ storeId }: { storeId: string }) {
  const [open, setOpen] = useState(false)
  const create = useCreateOrder(storeId)
  const form = useForm<Input_, unknown, z.output<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { externalId: "", amount: "", orderDate: today() },
  })

  const onSubmit = form.handleSubmit((values) =>
    create.mutate(values, {
      onSuccess: (order) => {
        toast.success(`Pedido ${order.externalId} cadastrado`)
        form.reset({ externalId: "", amount: "", orderDate: today() })
        setOpen(false)
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 409) {
          form.setError("externalId", { message: "Já existe um pedido com este código nesta loja" })
        } else {
          toast.error(describeError(error).title, { description: describeError(error).description })
        }
      },
    }),
  )

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <PlusIcon /> Novo pedido
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Novo pedido</DialogTitle>
          <DialogDescription>O código precisa ser o mesmo que o adquirente manda no arquivo de repasses.</DialogDescription>
        </DialogHeader>
        <form id="new-order" onSubmit={onSubmit} className="grid gap-4" noValidate>
          <Field id="order-id" label="Código do pedido" error={form.formState.errors.externalId?.message}>
            <Input id="order-id" className="font-mono" placeholder="PED-1008" {...form.register("externalId")} />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field id="order-amount" label="Valor (R$)" error={form.formState.errors.amount?.message}>
              <Input id="order-amount" inputMode="decimal" className="font-mono" placeholder="150,00" {...form.register("amount")} />
            </Field>
            <Field id="order-date" label="Data da venda" error={form.formState.errors.orderDate?.message}>
              <Input id="order-date" type="date" max={today()} {...form.register("orderDate")} />
            </Field>
          </div>
        </form>
        <DialogFooter>
          <Button type="submit" form="new-order" disabled={create.isPending}>
            {create.isPending ? "Salvando…" : "Cadastrar pedido"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function OrdersPage() {
  const { storeId = "" } = useParams()
  const [params, setParams] = useSearchParams()
  const status = (params.get("status") ?? ALL) as ReconciliationStatus | typeof ALL
  const page = Number(params.get("pagina") ?? 0)
  const query = useOrders(storeId, { status: status === ALL ? undefined : status, page })

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Pedidos"
        description="Vendas que a loja espera receber. Ficam no prazo até D+2; depois disso, sem repasse, viram divergência."
        actions={<NewOrderDialog storeId={storeId} />}
      />

      <Tabs value={status} onValueChange={(v) => setParams(v === ALL ? {} : { status: v }, { replace: true })}>
        <TabsList>
          <TabsTrigger value={ALL}>Todos</TabsTrigger>
          <TabsTrigger value="PENDING">No prazo</TabsTrigger>
          <TabsTrigger value="RECONCILED">Conciliados</TabsTrigger>
          <TabsTrigger value="DIVERGENT">Divergentes</TabsTrigger>
        </TabsList>
      </Tabs>

      {query.isError && <ErrorLine message={describeError(query.error).title} />}

      {query.isLoading ? (
        <Skeleton className="h-80" />
      ) : query.data && query.data.content.length === 0 ? (
        <Empty title="Nenhum pedido aqui">Cadastre um pedido ou mude o filtro.</Empty>
      ) : (
        <div>
          <div className="overflow-x-auto border border-border bg-card">
            <Table className="ledger-rows">
              <TableHeader>
                <TableRow>
                  <TableHead>Pedido</TableHead>
                  <TableHead>Data da venda</TableHead>
                  <TableHead className="text-right">Valor (R$)</TableHead>
                  <TableHead>Conciliação</TableHead>
                  <TableHead>Cadastrado em</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody className={query.isPlaceholderData ? "opacity-60" : undefined}>
                {query.data?.content.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono text-[13px] font-medium">{order.externalId}</TableCell>
                    <TableCell>{day(order.orderDate)}</TableCell>
                    <TableCell className="text-right">
                      <Amount value={order.amount} />
                    </TableCell>
                    <TableCell>
                      <OrderStatusLabel status={order.reconciliationStatus} />
                    </TableCell>
                    <TableCell className="font-mono text-[12px] text-muted-foreground tabular">{dateTime(order.createdAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Pager
            page={query.data?.page}
            noun="pedidos"
            onChange={(p) => {
              const next = new URLSearchParams(params)
              next.set("pagina", String(p))
              setParams(next, { replace: true })
            }}
          />
        </div>
      )}
    </div>
  )
}
