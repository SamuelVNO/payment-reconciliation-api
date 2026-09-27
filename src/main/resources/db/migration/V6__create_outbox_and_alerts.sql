-- Transactional Outbox: o evento é gravado na MESMA transação da divergência.
-- Um relay lê esta tabela e publica no Kafka. Sem dual write, sem evento perdido.
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(80)  NOT NULL,
    topic          VARCHAR(120) NOT NULL,
    message_key    VARCHAR(80)  NOT NULL,
    payload        JSONB        NOT NULL,
    attempts       INT          NOT NULL,
    last_error     VARCHAR(500),
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

-- Índice parcial: só as linhas ainda não publicadas (uma fração mínima da tabela)
CREATE INDEX ix_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- Lado consumidor: alertas por loja. event_id único = consumidor idempotente.
CREATE TABLE store_alerts (
    id              UUID PRIMARY KEY,
    store_id        UUID           NOT NULL REFERENCES stores (id),
    event_id        UUID           NOT NULL,
    discrepancy_id  UUID           NOT NULL,
    type            VARCHAR(30)    NOT NULL,
    difference      NUMERIC(19, 2) NOT NULL,
    message         VARCHAR(255)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_store_alerts_event UNIQUE (event_id)
);

CREATE INDEX ix_store_alerts_store_created ON store_alerts (store_id, created_at DESC);
