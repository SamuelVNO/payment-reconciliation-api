import { format, parseISO } from "date-fns"
import { ptBR } from "date-fns/locale"

const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })
const plain = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const percent = new Intl.NumberFormat("pt-BR", { style: "percent", minimumFractionDigits: 2 })
const integer = new Intl.NumberFormat("pt-BR")

export const MINUS = "−"

export function money(value: number) {
  return brl.format(value).replace("-", MINUS)
}

/** Número sem "R$", para colunas de tabela onde o cabeçalho já diz a moeda. */
export function amount(value: number) {
  return plain.format(value).replace("-", MINUS)
}

/** Diferença com sinal explícito: +0,81 / −20,00 */
export function signed(value: number) {
  if (value === 0) return plain.format(0)
  return (value > 0 ? "+" : MINUS) + plain.format(Math.abs(value))
}

export function rate(value: number) {
  return percent.format(value)
}

export function count(value: number) {
  return integer.format(value)
}

/** 12345678000190 -> 12.345.678/0001-90 */
export function cnpj(document: string) {
  return document.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, "$1.$2.$3/$4-$5")
}

export function day(iso: string) {
  return format(parseISO(iso), "dd MMM yyyy", { locale: ptBR })
}

export function dayShort(iso: string) {
  return format(parseISO(iso), "dd/MM", { locale: ptBR })
}

export function dateTime(iso: string) {
  return format(parseISO(iso), "dd/MM/yyyy HH:mm", { locale: ptBR })
}

export function time(iso: string) {
  return format(parseISO(iso), "HH:mm:ss", { locale: ptBR })
}

export function duration(startIso: string, endIso: string | null) {
  if (!endIso) return "em andamento"
  const ms = parseISO(endIso).getTime() - parseISO(startIso).getTime()
  return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1).replace(".", ",")} s`
}

/** "1.234,56" ou "1234.56" -> 1234.56. Devolve NaN se não for número. */
export function parseAmount(input: string) {
  const clean = input.trim().replace(/\s|R\$/g, "")
  const normalized = clean.includes(",") ? clean.replace(/\./g, "").replace(",", ".") : clean
  return /^\d+(\.\d{1,2})?$/.test(normalized) ? Number(normalized) : Number.NaN
}

/** "Taxa acima da contratada: pedido PED-1, diferença de R$ 0,81" -> "pedido PED-1, diferença de R$ 0,81" */
export function alertDetail(message: string) {
  const colon = message.indexOf(": ")
  return colon === -1 ? message : message.slice(colon + 2)
}
