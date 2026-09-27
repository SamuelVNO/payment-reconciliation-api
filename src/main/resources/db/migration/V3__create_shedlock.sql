-- Tabela do ShedLock: garante que um job agendado rode em uma única instância por vez.
-- TIMESTAMP sem fuso de propósito: com usingDbTime() o ShedLock grava timezone('utc', now()),
-- que é um TIMESTAMP "puro" em UTC. Numa coluna TIMESTAMPTZ o Postgres reinterpretaria esse
-- valor no fuso da sessão (America/Sao_Paulo) e o lock ficaria 3h no futuro.
CREATE TABLE shedlock (
    name       VARCHAR(64)  PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
