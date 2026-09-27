#!/usr/bin/env bash
# Roda o fluxo completo contra a API: loja -> pedidos -> CSV de repasses -> conciliação -> alertas.
# Pré-requisitos: API no ar (docker compose up --build), curl e jq.
set -euo pipefail

API="${API:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@local.dev}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin12345}"
CSV="$(dirname "$0")/../samples/payouts-2026-09.csv"

step() { printf '\n\033[1;36m==> %s\033[0m\n' "$1"; }

step "Login como ADMIN"
TOKEN=$(curl -sf "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | jq -r .accessToken)
AUTH="Authorization: Bearer $TOKEN"
echo "token: ${TOKEN:0:30}..."

step "Cadastrando loja com taxa contratada de 2,99%"
DOCUMENT=$(( $(date +%s) * 10000 + RANDOM % 10000 ))   # 14 dígitos, como um CNPJ
STORE_ID=$(curl -sf -X POST "$API/api/stores" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Loja Demo\",\"document\":\"$DOCUMENT\",\"feeRate\":0.0299}" | jq -r .id)
echo "loja: $STORE_ID"

step "Cadastrando pedidos"
TODAY=$(date +%Y-%m-%d)
order() {
  curl -sf -o /dev/null -X POST "$API/api/stores/$STORE_ID/orders" -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"externalId\":\"$1\",\"amount\":$2,\"orderDate\":\"$3\"}"
  echo "  $1  R\$ $2  ($3)"
}
order PED-1001 150.00 2026-09-20
order PED-1002 89.90 2026-09-20
order PED-1003 200.00 2026-09-21
order PED-1004 75.00 2026-09-21
order PED-1005 120.00 2026-09-22
order PED-1006 60.00 "$TODAY"

step "Importando CSV de repasses (duas vezes com a mesma Idempotency-Key)"
curl -sf -X POST "$API/api/stores/$STORE_ID/payout-imports" -H "$AUTH" -H "Idempotency-Key: demo-$DOCUMENT" \
  -F "file=@$CSV" | jq '{importedRows, duplicateRows, rejectedRows, errors}'
curl -sf -i -X POST "$API/api/stores/$STORE_ID/payout-imports" -H "$AUTH" -H "Idempotency-Key: demo-$DOCUMENT" \
  -F "file=@$CSV" | grep -i '^Idempotent-Replayed'

step "Rodando a conciliação"
curl -sf -X POST "$API/api/stores/$STORE_ID/reconciliation-runs" -H "$AUTH" \
  | jq '{ordersChecked, reconciled, divergent, stillPending, discrepanciesFound}'

step "Divergências encontradas"
curl -sf "$API/api/stores/$STORE_ID/discrepancies" -H "$AUTH" \
  | jq -r '.content[] | [.type, (.orderExternalId // "-"), (.payoutExternalId // "-"), .difference] | @tsv' | column -t

step "Aguardando o outbox publicar no Kafka e o consumidor gerar os alertas"
for _ in $(seq 1 15); do
  COUNT=$(curl -sf "$API/api/stores/$STORE_ID/alerts" -H "$AUTH" | jq '.page.totalElements')
  [ "$COUNT" -ge 5 ] && break
  sleep 1
done
curl -sf "$API/api/stores/$STORE_ID/alerts" -H "$AUTH" | jq -r '.content[].message'

step "Relatório do mês"
curl -sf "$API/api/stores/$STORE_ID/reconciliation-report?from=2026-09-01&to=2026-09-30" -H "$AUTH" \
  | jq '{ordersByStatus, openDiscrepancies, discrepanciesByType}'
