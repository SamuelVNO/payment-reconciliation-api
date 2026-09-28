import { Link } from "react-router"

import { Button } from "@/components/ui/button"

export function NotFoundPage() {
  return (
    <div className="grid min-h-[60svh] place-content-center gap-4 text-center">
      <p className="font-mono text-sm text-muted-foreground">404</p>
      <h1 className="font-heading text-2xl font-semibold">Esta página não existe</h1>
      <Button asChild variant="outline" className="justify-self-center">
        <Link to="/">Voltar para o início</Link>
      </Button>
    </div>
  )
}
