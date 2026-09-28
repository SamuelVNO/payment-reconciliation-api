import { useState, type ComponentType } from "react"
import { NavLink, Outlet, useNavigate, useParams } from "react-router"
import {
  ArrowsLeftRightIcon,
  BellSimpleIcon,
  ClockCounterClockwiseIcon,
  GaugeIcon,
  ListIcon,
  ShoppingBagOpenIcon,
  SignOutIcon,
  StorefrontIcon,
  TrayArrowDownIcon,
  WarningDiamondIcon,
  type IconProps,
} from "@phosphor-icons/react"

import { Brand } from "@/components/brand"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { useDiscrepancies, useStore } from "@/hooks/api"
import { useAuth, useSession } from "@/lib/use-auth"
import { cnpj, count, rate } from "@/lib/format"
import { cn } from "@/lib/utils"

type NavItem = { to: string; label: string; icon: ComponentType<IconProps>; end?: boolean; badge?: number }

function StoreBlock({ storeId }: { storeId: string }) {
  const store = useStore(storeId)
  const session = useSession()
  return (
    <div className="grid gap-1 border-y border-sidebar-border px-4 py-4">
      <span className="text-[10px] font-semibold uppercase tracking-[0.14em] text-sidebar-foreground/70">Loja</span>
      {store.data ? (
        <>
          <span className="truncate text-[15px] font-semibold text-sidebar-accent-foreground">{store.data.name}</span>
          <span className="font-mono text-[11px] tabular">{cnpj(store.data.document)}</span>
          <span className="font-mono text-[11px] tabular">taxa contratada {rate(store.data.feeRate)}</span>
        </>
      ) : (
        <Skeleton className="h-12 bg-sidebar-accent" />
      )}
      {session.role === "ADMIN" && (
        <NavLink
          to="/lojas"
          className="mt-2 inline-flex items-center gap-1.5 text-xs text-sidebar-primary hover:underline"
        >
          <ArrowsLeftRightIcon className="size-3.5" /> Trocar de loja
        </NavLink>
      )}
    </div>
  )
}

function Nav({ items, onNavigate }: { items: NavItem[]; onNavigate?: () => void }) {
  return (
    <nav className="grid gap-0.5 px-2 py-3" aria-label="Seções">
      {items.map(({ to, label, icon: Icon, end, badge }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "group flex h-9 items-center gap-3 px-3 text-[14px] transition-colors",
              "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
              isActive && "bg-sidebar-accent text-sidebar-accent-foreground shadow-[inset_2px_0_0_var(--sidebar-primary)]",
            )
          }
        >
          <Icon className="size-[18px] shrink-0" />
          <span className="flex-1">{label}</span>
          {badge ? (
            <span className="min-w-5 bg-loss/90 px-1.5 text-center font-mono text-[11px] leading-5 text-white tabular">
              {count(badge)}
            </span>
          ) : null}
        </NavLink>
      ))}
    </nav>
  )
}

function UserBlock() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  if (!session) return null
  return (
    <div className="mt-auto grid gap-2 border-t border-sidebar-border px-4 py-4">
      <div className="grid">
        <span className="truncate text-[13px] text-sidebar-accent-foreground">{session.email}</span>
        <span className="font-mono text-[10px] uppercase tracking-wider">
          {session.role === "ADMIN" ? "administrador" : "usuário da loja"}
        </span>
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="sm"
          className="-ml-2 text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
          onClick={() => {
            logout()
            navigate("/login")
          }}
        >
          <SignOutIcon /> Sair
        </Button>
        <ThemeToggle className="ml-auto text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground" />
      </div>
    </div>
  )
}

function SidebarContent({ storeId, onNavigate }: { storeId?: string; onNavigate?: () => void }) {
  const session = useSession()
  const open = useDiscrepancies(storeId ?? "", { status: "OPEN", page: 0, size: 1 })
  const openCount = storeId ? open.data?.page.totalElements : undefined

  const storeItems: NavItem[] = storeId
    ? [
        { to: `/lojas/${storeId}`, label: "Visão geral", icon: GaugeIcon, end: true },
        { to: `/lojas/${storeId}/divergencias`, label: "Divergências", icon: WarningDiamondIcon, badge: openCount },
        { to: `/lojas/${storeId}/pedidos`, label: "Pedidos", icon: ShoppingBagOpenIcon },
        { to: `/lojas/${storeId}/repasses`, label: "Repasses", icon: TrayArrowDownIcon },
        { to: `/lojas/${storeId}/execucoes`, label: "Execuções", icon: ClockCounterClockwiseIcon },
        { to: `/lojas/${storeId}/alertas`, label: "Alertas", icon: BellSimpleIcon },
      ]
    : []
  const adminItems: NavItem[] =
    session.role === "ADMIN" && !storeId ? [{ to: "/lojas", label: "Lojas", icon: StorefrontIcon, end: true }] : []

  return (
    <div className="flex h-full flex-col bg-sidebar text-sidebar-foreground">
      <Brand className="h-14 px-4 text-sidebar-accent-foreground" />
      {storeId && <StoreBlock storeId={storeId} />}
      <Nav items={[...adminItems, ...storeItems]} onNavigate={onNavigate} />
      <UserBlock />
    </div>
  )
}

export function AppShell() {
  const { storeId } = useParams()
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="min-h-svh bg-background lg:grid lg:grid-cols-[248px_minmax(0,1fr)]">
      <aside className="hidden bg-sidebar lg:block">
        <div className="sticky top-0 h-svh">
          <SidebarContent storeId={storeId} />
        </div>
      </aside>

      <div className="sticky top-0 z-20 flex h-12 items-center justify-between border-b border-sidebar-border bg-sidebar px-3 text-sidebar-accent-foreground lg:hidden">
        <Brand />
        <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label="Abrir menu" className="hover:bg-sidebar-accent">
              <ListIcon />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-[272px] border-sidebar-border p-0">
            <SheetTitle className="sr-only">Menu</SheetTitle>
            <SidebarContent storeId={storeId} onNavigate={() => setMenuOpen(false)} />
          </SheetContent>
        </Sheet>
      </div>

      <main className="mx-auto w-full max-w-[1180px] px-4 py-6 sm:px-8 sm:py-8">
        <Outlet />
      </main>
    </div>
  )
}
