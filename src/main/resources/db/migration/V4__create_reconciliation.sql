CREATE TABLE reconciliation_runs (
    id                   UUID PRIMARY KEY,
    store_id             UUID        NOT NULL REFERENCES stores (id),
    trigger_source       VARCHAR(20) NOT NULL,
    business_date        DATE        NOT NULL,
    orders_checked       INT         NOT NULL,
    reconciled           INT         NOT NULL,
    divergent            INT         NOT NULL,
    still_pending        INT         NOT NULL,
    discrepancies_found  INT         NOT NULL,
    finished_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_reconciliation_runs_store_created ON reconciliation_runs (store_id, created_at DESC);

CREATE TABLE discrepancies (
    id               UUID PRIMARY KEY,
    store_id         UUID           NOT NULL REFERENCES stores (id),
    run_id           UUID           NOT NULL REFERENCES reconciliation_runs (id),
    order_id         UUID           REFERENCES orders (id),
    payout_id        UUID           REFERENCES payouts (id),
    type             VARCHAR(30)    NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    expected_amount  NUMERIC(19, 2) NOT NULL,
    actual_amount    NUMERIC(19, 2) NOT NULL,
    difference       NUMERIC(19, 2) NOT NULL,
    description      VARCHAR(255)   NOT NULL,
    resolution_note  VARCHAR(255),
    resolved_at      TIMESTAMPTZ,
    version          BIGINT         NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_discrepancies_target CHECK (order_id IS NOT NULL OR payout_id IS NOT NULL),
    -- Postgres 15+: NULLS NOT DISTINCT faz (tipo, pedido, NULL) colidir com outro (tipo, pedido, NULL).
    -- É o que torna o motor idempotente: rodar duas vezes não duplica divergências.
    CONSTRAINT uk_discrepancies_type_order_payout UNIQUE NULLS NOT DISTINCT (type, order_id, payout_id)
);

-- Listagem da tela: "divergências abertas da loja X, mais recentes primeiro"
CREATE INDEX ix_discrepancies_store_status_created ON discrepancies (store_id, status, created_at DESC);
-- NOT EXISTS da busca de repasses órfãos
CREATE INDEX ix_discrepancies_payout ON discrepancies (payout_id) WHERE payout_id IS NOT NULL;

-- Lote do motor: WHERE store_id = ? AND reconciliation_status = 'PENDING' AND id > ? ORDER BY id LIMIT 500
CREATE INDEX ix_orders_store_status_id ON orders (store_id, reconciliation_status, id);
-- Cruzamento pedido x repasse pelo id externo do pedido
CREATE INDEX ix_payouts_store_order_external_id ON payouts (store_id, order_external_id);
