CREATE TABLE payout_imports (
    id              UUID PRIMARY KEY,
    store_id        UUID         NOT NULL REFERENCES stores (id),
    idempotency_key VARCHAR(100) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    total_rows      INT          NOT NULL,
    imported_rows   INT          NOT NULL,
    duplicate_rows  INT          NOT NULL,
    rejected_rows   INT          NOT NULL,
    errors          JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_payout_imports_store_key UNIQUE (store_id, idempotency_key)
);

CREATE TABLE payouts (
    id                UUID PRIMARY KEY,
    store_id          UUID           NOT NULL REFERENCES stores (id),
    import_id         UUID           NOT NULL REFERENCES payout_imports (id),
    external_id       VARCHAR(64)    NOT NULL,
    order_external_id VARCHAR(64)    NOT NULL,
    gross_amount      NUMERIC(19, 2) NOT NULL,
    fee_amount        NUMERIC(19, 2) NOT NULL,
    net_amount        NUMERIC(19, 2) NOT NULL,
    paid_at           DATE           NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_payouts_store_external_id UNIQUE (store_id, external_id)
);

CREATE INDEX ix_payouts_import_id ON payouts (import_id);
