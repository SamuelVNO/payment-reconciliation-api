import type { ProblemDetail } from "./types"

const TOKEN_KEY = "conciliador.token"

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // armazenamento bloqueado: a sessão dura só enquanto a aba estiver aberta
  }
}

/** Erro da API já no formato RFC 9457 que o backend devolve. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`)
    this.status = status
    this.problem = problem
  }

  fieldErrors(): Record<string, string> {
    return Object.fromEntries((this.problem.errors ?? []).map((e) => [e.field, e.message]))
  }
}

let onUnauthorized: () => void = () => {}

/** O AuthProvider registra aqui o que fazer quando o token expira. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

type RequestOptions = {
  method?: string
  body?: unknown
  headers?: Record<string, string>
  query?: Record<string, string | number | undefined | null>
}

export type ApiResponse<T> = { data: T; headers: Headers }

export async function requestWithHeaders<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(options.query ?? {})) {
    if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, String(value))
  }

  const headers: Record<string, string> = { Accept: "application/json", ...options.headers }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let body: BodyInit | undefined
  if (options.body instanceof FormData) {
    body = options.body
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json"
    body = JSON.stringify(options.body)
  }

  const response = await fetch(url, { method: options.method ?? "GET", headers, body })

  if (response.status === 401 && token) {
    onUnauthorized()
  }
  if (!response.ok) {
    let problem: ProblemDetail = { status: response.status }
    try {
      problem = await response.json()
    } catch {
      // corpo vazio ou não-JSON (proxy fora do ar, por exemplo)
    }
    throw new ApiError(response.status, problem)
  }
  const data = response.status === 204 ? (undefined as T) : ((await response.json()) as T)
  return { data, headers: response.headers }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return (await requestWithHeaders<T>(path, options)).data
}

/** Mensagem em português para o toast, com o detalhe técnico da API quando existir. */
export function describeError(error: unknown): { title: string; description?: string } {
  if (error instanceof ApiError) {
    const byStatus: Record<number, string> = {
      400: "Dados inválidos",
      401: "Sessão expirada, entre de novo",
      403: "Você não tem acesso a esta loja",
      404: "Não encontrado",
      409: "Conflito com dados existentes",
    }
    return {
      title: byStatus[error.status] ?? "A API respondeu com erro",
      description: error.problem.detail,
    }
  }
  return { title: "Sem conexão com a API", description: "Verifique se o backend está rodando na porta 8080." }
}
