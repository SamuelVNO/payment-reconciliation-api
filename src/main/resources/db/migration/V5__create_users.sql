CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    store_id      UUID         REFERENCES stores (id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    -- ADMIN enxerga todas as lojas; STORE_USER pertence a exatamente uma
    CONSTRAINT ck_users_role_store CHECK (
        (role = 'ADMIN' AND store_id IS NULL) OR (role = 'STORE_USER' AND store_id IS NOT NULL))
);
