import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Link } from "react-router"
import { ArrowRightIcon, PlusIcon, UserPlusIcon } from "@phosphor-icons/react"
import { toast } from "sonner"
import { z } from "zod"

import { Empty, ErrorLine } from "@/components/empty"
import { Field } from "@/components/field"
import { PageHeader } from "@/components/page-header"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useCreateStore, useCreateStoreUser, useStores } from "@/hooks/api"
import { ApiError, describeError } from "@/lib/api"
import { cnpj, day, parseAmount, rate } from "@/lib/format"
import type { Store } from "@/lib/types"

const storeSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome").max(120),
  document: z
    .string()
    .transform((v) => v.replace(/\D/g, ""))
    .pipe(z.string().length(14, "CNPJ precisa ter 14 dígitos")),
  feeRate: z
    .string()
    .transform((v) => parseAmount(v))
    .pipe(z.number({ error: "Use um número como 2,99" }).min(0).lt(100, "A taxa precisa ser menor que 100%")),
})
type StoreInput = z.input<typeof storeSchema>

function NewStoreDialog() {
  const [open, setOpen] = useState(false)
  const create = useCreateStore()
  const form = useForm<StoreInput, unknown, z.output<typeof storeSchema>>({
    resolver: zodResolver(storeSchema),
    defaultValues: { name: "", document: "", feeRate: "2,99" },
  })

  const onSubmit = form.handleSubmit((values) =>
    create.mutate(
      { name: values.name, document: values.document, feeRate: Number((values.feeRate / 100).toFixed(4)) },
      {
        onSuccess: (store) => {
          toast.success(`Loja ${store.name} cadastrada`)
          form.reset()
          setOpen(false)
        },
        onError: (error) => {
          if (error instanceof ApiError && error.status === 409) {
            form.setError("document", { message: "Já existe uma loja com este CNPJ" })
          } else {
            toast.error(describeError(error).title, { description: describeError(error).description })
          }
        },
      },
    ),
  )

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <PlusIcon /> Nova loja
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Nova loja</DialogTitle>
          <DialogDescription>A taxa contratada é usada para detectar cobrança acima do combinado.</DialogDescription>
        </DialogHeader>
        <form id="new-store" onSubmit={onSubmit} className="grid gap-4" noValidate>
          <Field id="store-name" label="Nome" error={form.formState.errors.name?.message}>
            <Input id="store-name" {...form.register("name")} />
          </Field>
          <Field id="store-document" label="CNPJ" error={form.formState.errors.document?.message}>
            <Input id="store-document" inputMode="numeric" placeholder="12.345.678/0001-90" {...form.register("document")} />
          </Field>
          <Field id="store-fee" label="Taxa contratada (%)" error={form.formState.errors.feeRate?.message} hint="Ex.: 2,99 para 2,99% por transação">
            <Input id="store-fee" inputMode="decimal" className="font-mono" {...form.register("feeRate")} />
          </Field>
        </form>
        <DialogFooter>
          <Button type="submit" form="new-store" disabled={create.isPending}>
            {create.isPending ? "Salvando…" : "Cadastrar loja"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const userSchema = z.object({
  email: z.email("Informe um e-mail válido"),
  password: z.string().min(10, "Mínimo de 10 caracteres").max(72, "Máximo de 72 caracteres"),
})
type UserInput = z.infer<typeof userSchema>

function NewUserDialog({ store }: { store: Store }) {
  const [open, setOpen] = useState(false)
  const create = useCreateStoreUser()
  const form = useForm<UserInput>({ resolver: zodResolver(userSchema), defaultValues: { email: "", password: "" } })

  const onSubmit = form.handleSubmit((values) =>
    create.mutate(
      { ...values, storeId: store.id },
      {
        onSuccess: (user) => {
          toast.success("Usuário criado", { description: `${user.email} já pode entrar e só enxerga a ${store.name}.` })
          form.reset()
          setOpen(false)
        },
        onError: (error) => {
          if (error instanceof ApiError && error.status === 409) {
            form.setError("email", { message: "Este e-mail já está em uso" })
          } else {
            toast.error(describeError(error).title, { description: describeError(error).description })
          }
        },
      },
    ),
  )

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm">
          <UserPlusIcon /> Usuário
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Usuário da {store.name}</DialogTitle>
          <DialogDescription>Ele entra com e-mail e senha e só tem acesso aos dados desta loja.</DialogDescription>
        </DialogHeader>
        <form id={`new-user-${store.id}`} onSubmit={onSubmit} className="grid gap-4" noValidate>
          <Field id={`user-email-${store.id}`} label="E-mail" error={form.formState.errors.email?.message}>
            <Input id={`user-email-${store.id}`} type="email" {...form.register("email")} />
          </Field>
          <Field id={`user-password-${store.id}`} label="Senha inicial" error={form.formState.errors.password?.message}>
            <Input id={`user-password-${store.id}`} type="password" autoComplete="new-password" {...form.register("password")} />
          </Field>
        </form>
        <DialogFooter>
          <Button type="submit" form={`new-user-${store.id}`} disabled={create.isPending}>
            {create.isPending ? "Criando…" : "Criar usuário"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function StoresPage() {
  const stores = useStores()

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Lojas"
        description="Cada loja é um tenant: tem os próprios pedidos, repasses e divergências, e seus usuários não enxergam as outras."
        actions={<NewStoreDialog />}
      />

      {stores.isError && <ErrorLine message={describeError(stores.error).title} />}

      {stores.isLoading ? (
        <Skeleton className="h-64" />
      ) : stores.data && stores.data.content.length === 0 ? (
        <Empty title="Nenhuma loja cadastrada">Cadastre a primeira loja para importar repasses e conciliar.</Empty>
      ) : (
        <div className="border border-border bg-card">
          <Table className="ledger-rows">
            <TableHeader>
              <TableRow>
                <TableHead>Loja</TableHead>
                <TableHead>CNPJ</TableHead>
                <TableHead className="text-right">Taxa</TableHead>
                <TableHead>Desde</TableHead>
                <TableHead className="w-0" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {stores.data?.content.map((store) => (
                <TableRow key={store.id}>
                  <TableCell className="font-medium">
                    <Link to={`/lojas/${store.id}`} className="hover:underline">
                      {store.name}
                    </Link>
                  </TableCell>
                  <TableCell className="font-mono text-[13px] tabular">{cnpj(store.document)}</TableCell>
                  <TableCell className="text-right font-mono text-[13px] tabular">{rate(store.feeRate)}</TableCell>
                  <TableCell className="text-muted-foreground">{day(store.createdAt)}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <NewUserDialog store={store} />
                      <Button variant="outline" size="sm" asChild>
                        <Link to={`/lojas/${store.id}`}>
                          Abrir <ArrowRightIcon />
                        </Link>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
