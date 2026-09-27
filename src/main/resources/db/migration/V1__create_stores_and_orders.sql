CREATE TABLE stores (
    id         UUID PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    document   VARCHAR(14)  NOT NULL,
    fee_rate   NUMERIC(5, 4) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_stores_document UNIQUE (document),
    CONSTRAINT ck_stores_fee_rate CHECK (fee_rate >= 0 AND fee_rate < 1)
);

CREATE TABLE orders (
    id                    UUID PRIMARY KEY,
    store_id              UUID          NOT NULL REFERENCES stores (id),
    external_id           VARCHAR(64)   NOT NULL,
    amount                NUMERIC(19, 2) NOT NULL,
    order_date            DATE          NOT NULL,
    reconciliation_status VARCHAR(20)   NOT NULL,
    version               BIGINT        NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_orders_store_external_id UNIQUE (store_id, external_id),
    CONSTRAINT ck_orders_amount CHECK (amount > 0)
);
