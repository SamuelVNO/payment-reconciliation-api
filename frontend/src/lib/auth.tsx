import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { useQueryClient } from "@tanstack/react-query"

import { getToken, request, setToken, setUnauthorizedHandler } from "./api"
import type { TokenResponse } from "./types"
import { AuthContext, type Session } from "./use-auth"

// O payload do JWT é só Base64URL: dá para ler no navegador. Quem garante a validade é a API.
function decode(token: string): Session | null {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")
    const bytes = Uint8Array.from(atob(payload), (char) => char.charCodeAt(0))
    const json = JSON.parse(new TextDecoder().decode(bytes))
    const session: Session = {
      token,
      userId: json.sub,
      email: json.email,
      role: json.role,
      storeId: json.store_id ?? null,
      expiresAt: json.exp * 1000,
    }
    return session.expiresAt > Date.now() ? session : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => {
    const token = getToken()
    return token ? decode(token) : null
  })
  const [expiredNotice, setExpiredNotice] = useState(false)

  const logout = useCallback(
    (reason?: "expired") => {
      setToken(null)
      setSession(null)
      setExpiredNotice(reason === "expired")
      queryClient.clear()
    },
    [queryClient],
  )

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await request<TokenResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password },
    })
    const next = decode(accessToken)
    if (!next) throw new Error("Token inválido recebido da API")
    setToken(accessToken)
    setSession(next)
    setExpiredNotice(false)
    return next
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => logout("expired"))
  }, [logout])

  // Desloga sozinho quando o token vence, mesmo sem nenhuma requisição acontecer
  useEffect(() => {
    if (!session) return
    const timer = window.setTimeout(() => logout("expired"), session.expiresAt - Date.now())
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(() => ({ session, login, logout, expiredNotice }), [session, login, logout, expiredNotice])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
