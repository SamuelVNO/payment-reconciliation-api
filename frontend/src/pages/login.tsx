import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Navigate, useNavigate } from "react-router"
import { z } from "zod"

import { Brand } from "@/components/brand"
import { ErrorLine } from "@/components/empty"
import { Field } from "@/components/field"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ApiError } from "@/lib/api"
import { useAuth } from "@/lib/use-auth"
import { cn } from "@/lib/utils"

const schema = z.object({
  email: z.email("Informe um e-mail válido"),
  password: z.string().min(1, "Informe a senha"),
})
type FormValues = z.infer<typeof schema>

const DEMO = { email: "admin@local.dev", password: "admin12345" }

// Exemplo real do motor com samples/payouts-2026-09.csv
const TAPE: { order: string; payout: string; note: string; tone: "ok" | "loss" | "surplus" }[] = [
  { order: "PED-1001  150,00", payout: "PAY-5001  150,00", note: "conciliado", tone: "ok" },
  { order: "PED-1002   89,90", payout: "PAY-5002  taxa 3,50", note: "taxa acima de 2,69", tone: "loss" },
  { order: "PED-1003  200,00", payout: "PAY-5003  180,00", note: "−20,00", tone: "loss" },
  { order: "PED-1004   75,00", payout: "—", note: "D+2 vencido", tone: "loss" },
  { order: "PED-1005  120,00", payout: "PAY-5006  120,00", note: "pago em dobro", tone: "surplus" },
  { order: "—", payout: "PAY-5007   50,00", note: "sem pedido", tone: "surplus" },
]

const toneText = { ok: "text-[#7ed2c2]", loss: "text-[#f0908a]", surplus: "text-[#e8b86b]" }

function Tape() {
  return (
    <div className="relative hidden flex-col justify-center gap-12 overflow-hidden bg-[#0f1f1d] p-10 text-[#b9c8c3] lg:flex xl:p-16">
      <div className="max-w-md">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#7ed2c2]">Conciliação de repasses</p>
        <p className="mt-4 font-heading text-[28px] font-semibold leading-tight text-[#f1f7f5]">
          Pedidos de um lado, repasses do adquirente do outro. O que não bate vira divergência.
        </p>
      </div>

      <figure className="w-full max-w-xl border border-[#213633] bg-[#0b1716] font-mono text-[12px] leading-[1.9]">
        <figcaption className="flex justify-between border-b border-dashed border-[#2a403c] px-4 py-2 text-[11px] uppercase tracking-wider">
          <span>Execução 27/09 03:00</span>
          <span>taxa 2,99%</span>
        </figcaption>
        <ol className="px-4 py-2">
          {TAPE.map((line) => (
            <li key={line.order + line.payout} className="grid grid-cols-[17ch_19ch_minmax(0,1fr)] gap-3 whitespace-pre">
              <span>{line.order}</span>
              <span>{line.payout}</span>
              <span className={cn("text-right", toneText[line.tone])}>{line.note}</span>
            </li>
          ))}
        </ol>
        <div className="border-t border-dashed border-[#2a403c] px-4 py-2 text-[11px] uppercase tracking-wider">
          1 conciliado · 4 divergentes · 1 órfão
        </div>
      </figure>

      <p className="absolute bottom-8 left-10 text-xs text-[#7f928d] xl:left-16">
        Java 21 · Spring Boot 3.5 · PostgreSQL · Kafka. Este painel consome a API REST do projeto.
      </p>
    </div>
  )
}

export function LoginPage() {
  const { session, login, expiredNotice } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } })

  if (session) return <Navigate to="/" replace />

  const onSubmit = form.handleSubmit(async (values) => {
    setError(null)
    try {
      await login(values.email, values.password)
      navigate("/", { replace: true })
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 401
          ? "E-mail ou senha incorretos."
          : "Não foi possível falar com a API. Ela está rodando na porta 8080?",
      )
    }
  })

  return (
    <div className="grid min-h-svh bg-background lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <div className="flex flex-col px-6 py-6 sm:px-12">
        <div className="flex items-center justify-between">
          <Brand />
          <ThemeToggle />
        </div>

        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-8 py-12">
          <div className="grid gap-2">
            <h1 className="font-heading text-3xl font-semibold tracking-tight">Entrar</h1>
            <p className="text-sm text-muted-foreground">Use sua conta de administrador ou de usuário da loja.</p>
          </div>

          {expiredNotice && <ErrorLine message="Sua sessão expirou. Entre de novo para continuar." />}

          <form onSubmit={onSubmit} className="grid gap-4" noValidate>
            <Field id="email" label="E-mail" error={form.formState.errors.email?.message}>
              <Input id="email" type="email" autoComplete="username" className="h-10" {...form.register("email")} />
            </Field>
            <Field id="password" label="Senha" error={form.formState.errors.password?.message}>
              <Input id="password" type="password" autoComplete="current-password" className="h-10" {...form.register("password")} />
            </Field>
            {error && <ErrorLine message={error} />}
            <Button type="submit" size="lg" className="mt-2 h-10" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? "Entrando…" : "Entrar"}
            </Button>
          </form>

          <div className="grid gap-2 border border-dashed border-border p-4 text-sm">
            <p className="font-medium">Ambiente de demonstração</p>
            <p className="text-muted-foreground">
              Admin criado pelo <code className="font-mono text-xs">docker compose</code>:{" "}
              <span className="font-mono text-xs text-foreground">{DEMO.email}</span> /{" "}
              <span className="font-mono text-xs text-foreground">{DEMO.password}</span>
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="justify-self-start"
              onClick={() => form.reset(DEMO)}
            >
              Preencher
            </Button>
          </div>
        </div>
      </div>
      <Tape />
    </div>
  )
}
