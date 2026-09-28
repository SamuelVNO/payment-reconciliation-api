import { createBrowserRouter } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Home, RequireAdmin, RequireAuth, RequireStoreAccess } from "@/components/guards"
import { LoginPage } from "@/pages/login"
import { NotFoundPage } from "@/pages/not-found"

// Os guards ficam ACIMA do AppShell: a barra lateral busca dados da loja, e não pode
// disparar nenhuma requisição para uma loja que o usuário não pode ver.
// Cada página vira um chunk separado: o login não baixa o código das telas internas.
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      { index: true, element: <Home /> },
      {
        element: <RequireAdmin />,
        children: [
          {
            element: <AppShell />,
            children: [{ path: "lojas", lazy: async () => ({ Component: (await import("@/pages/stores")).StoresPage }) }],
          },
        ],
      },
      {
        path: "lojas/:storeId",
        element: <RequireStoreAccess />,
        children: [
          {
            element: <AppShell />,
            children: [
              { index: true, lazy: async () => ({ Component: (await import("@/pages/overview")).OverviewPage }) },
              { path: "divergencias", lazy: async () => ({ Component: (await import("@/pages/discrepancies")).DiscrepanciesPage }) },
              { path: "pedidos", lazy: async () => ({ Component: (await import("@/pages/orders")).OrdersPage }) },
              { path: "repasses", lazy: async () => ({ Component: (await import("@/pages/payouts")).PayoutsPage }) },
              { path: "execucoes", lazy: async () => ({ Component: (await import("@/pages/runs")).RunsPage }) },
              { path: "alertas", lazy: async () => ({ Component: (await import("@/pages/alerts")).AlertsPage }) },
            ],
          },
        ],
      },
      { element: <AppShell />, children: [{ path: "*", element: <NotFoundPage /> }] },
    ],
  },
])
