import { createContext, useContext } from "react"

import type { Role } from "./types"

export type Session = {
  token: string
  userId: string
  email: string
  role: Role
  storeId: string | null
  expiresAt: number
}

type AuthContextValue = {
  session: Session | null
  login: (email: string, password: string) => Promise<Session>
  logout: (reason?: "expired") => void
  expiredNotice: boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth fora do AuthProvider")
  return ctx
}

export function useSession(): Session {
  const { session } = useAuth()
  if (!session) throw new Error("useSession sem usuário logado")
  return session
}
