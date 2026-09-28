import { Navigate, Outlet, useParams } from "react-router"

import { useAuth } from "@/lib/use-auth"

export function RequireAuth() {
  const { session } = useAuth()
  return session ? <Outlet /> : <Navigate to="/login" replace />
}

export function RequireAdmin() {
  const { session } = useAuth()
  if (session?.role === "ADMIN") return <Outlet />
  return <Navigate to={session?.storeId ? `/lojas/${session.storeId}` : "/login"} replace />
}

/** Usuário de loja que digita a URL de outra loja volta para a dele (a API também bloquearia com 403). */
export function RequireStoreAccess() {
  const { session } = useAuth()
  const { storeId } = useParams()
  if (session?.role === "STORE_USER" && session.storeId !== storeId) {
    return <Navigate to={`/lojas/${session.storeId}`} replace />
  }
  return <Outlet />
}

export function Home() {
  const { session } = useAuth()
  return <Navigate to={session?.role === "ADMIN" ? "/lojas" : `/lojas/${session?.storeId}`} replace />
}
