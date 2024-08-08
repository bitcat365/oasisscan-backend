CREATE DATABASE oasis;

CREATE TABLE IF NOT EXISTS block
(
    id               SERIAL PRIMARY KEY,
    height           INTEGER   NOT NULL,
    epoch            INTEGER   NOT NULL,
    hash             VARCHAR   NOT NULL,
    timestamp        TIMESTAMP NOT NULL,
    txs              INTEGER   NOT NULL,
    proposer_address VARCHAR   NOT NULL,
    meta             JSON      NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS block_height_uniq ON block (height);
CREATE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp);
CREATE INDEX IF NOT EXISTS block_proposer_address_idx ON block (proposer_address);

CREATE TABLE IF NOT EXISTS block_signature
(
    id                SERIAL PRIMARY KEY,
    height            INTEGER   NOT NULL,
    block_id_flag     INTEGER   NOT NULL,
    validator_address VARCHAR   NOT NULL,
    timestamp         TIMESTAMP NOT NULL,
    signature         VARCHAR   NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_height_validator_address UNIQUE (height, validator_address)
);
CREATE INDEX IF NOT EXISTS block_signature_validator_address_idx ON block_signature (validator_address);
CREATE INDEX IF NOT EXISTS block_signature_timestamp_idx ON block_signature (timestamp);

CREATE TABLE IF NOT EXISTS delegator
(
    id         SERIAL PRIMARY KEY,
    validator  VARCHAR   NOT NULL,
    delegator  VARCHAR   NOT NULL,
    shares     BIGINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS delegator_uniq ON delegator (validator, delegator);

CREATE TABLE IF NOT EXISTS node
(
    id                SERIAL PRIMARY KEY,
    entity_id         VARCHAR   NOT NULL,
    node_id           VARCHAR   NOT NULL,
    consensus_address VARCHAR   NOT NULL,
    height            INTEGER   NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_entity_id_node_id_consensus_address UNIQUE (entity_id, node_id, consensus_address)
);
CREATE INDEX IF NOT EXISTS node_height_idx ON node (height);

CREATE TABLE IF NOT EXISTS reward
(
    id                SERIAL PRIMARY KEY,
    delegator         VARCHAR   NOT NULL,
    validator         VARCHAR   NOT NULL,
    epoch             INTEGER   NOT NULL,
    delegation_amount BIGINT    NOT NULL DEFAULT 0,
    delegation_shares BIGINT    NOT NULL DEFAULT 0,
    reward            BIGINT    NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_delegator_validator_epoch UNIQUE (delegator, validator, epoch)
);
CREATE INDEX IF NOT EXISTS reward_delegator_idx ON reward (delegator);
CREATE INDEX IF NOT EXISTS reward_created_at_idx ON reward (created_at);
CREATE MATERIALIZED VIEW reward_days AS
SELECT DISTINCT DATE_TRUNC('day', created_at) AS day
FROM reward;

CREATE TABLE IF NOT EXISTS system_property
(
    id         SERIAL PRIMARY KEY,
    property   VARCHAR   NOT NULL UNIQUE,
    value      VARCHAR   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS transaction
(
    id         SERIAL PRIMARY KEY,
    tx_hash    VARCHAR   NOT NULL,
    method     VARCHAR   NOT NULL,
    status     BOOLEAN   NOT NULL,
    nonce      INTEGER   NOT NULL,
    height     INTEGER   NOT NULL,
    timestamp  TIMESTAMP NOT NULL,
    sign_addr  VARCHAR   NOT NULL,
    to_addr    VARCHAR   NOT NULL,
    fee        BIGINT    NOT NULL DEFAULT 0,
    amount     BIGINT    NOT NULL DEFAULT 0,
    shares     BIGINT    NOT NULL DEFAULT 0,
    error      JSON      NOT NULL,
    events     JSON      NOT NULL,
    raw        JSON      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_tx_hash_height UNIQUE (tx_hash, height)
);
CREATE INDEX IF NOT EXISTS transaction_tx_hash_idx ON transaction (tx_hash);
CREATE INDEX IF NOT EXISTS transaction_method_idx ON transaction (method);
CREATE INDEX IF NOT EXISTS transaction_height_idx ON transaction (height);
CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp);
CREATE INDEX IF NOT EXISTS transaction_sign_addr_idx ON transaction (sign_addr);
CREATE INDEX IF NOT EXISTS transaction_to_addr_idx ON transaction (to_addr);
CREATE MATERIALIZED VIEW daily_transaction_counts AS
SELECT DATE_TRUNC('day', timestamp) AS day,
       COUNT(id)                    AS count
FROM transaction
GROUP BY DATE_TRUNC('day', timestamp);

CREATE TABLE IF NOT EXISTS validator
(
    id                SERIAL PRIMARY KEY,
    entity_id         VARCHAR   NOT NULL UNIQUE,
    entity_address    VARCHAR   NOT NULL,
    node_id           VARCHAR   NOT NULL,
    node_address      VARCHAR   NOT NULL,
    consensus_address VARCHAR   NOT NULL,
    name              VARCHAR   NOT NULL,
    icon              VARCHAR   NOT NULL,
    website           VARCHAR   NOT NULL,
    twitter           VARCHAR   NOT NULL,
    keybase           VARCHAR   NOT NULL,
    email             VARCHAR   NOT NULL,
    description       VARCHAR   NOT NULL,
    escrow            BIGINT    NOT NULL DEFAULT 0,
    escrow_24h        BIGINT    NOT NULL DEFAULT 0,
    balance           BIGINT    NOT NULL DEFAULT 0,
    total_shares      BIGINT    NOT NULL DEFAULT 0,
    signs             INTEGER   NOT NULL DEFAULT 0,
    proposals         INTEGER   NOT NULL DEFAULT 0,
    score             INTEGER   NOT NULL DEFAULT 0,
    signs_uptime      INTEGER   NOT NULL DEFAULT 0,
    nodes             INTEGER   NOT NULL DEFAULT 0,
    status            BOOLEAN   NOT NULL,
    commission        INTEGER   NOT NULL DEFAULT 0,
    delegators        INTEGER   NOT NULL DEFAULT 0,
    rank              INTEGER   NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS runtime
(
    id         SERIAL PRIMARY KEY,
    runtime_id VARCHAR   NOT NULL UNIQUE,
    name       VARCHAR   NOT NULL,
    status     INTEGER   NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS runtime_round
(
    id            SERIAL PRIMARY KEY,
    runtime_id    VARCHAR   NOT NULL,
    round         INTEGER   NOT NULL,
    version       INTEGER   NOT NULL,
    timestamp     TIMESTAMP NOT NULL,
    header_type   INTEGER   NOT NULL,
    previous_hash VARCHAR   NOT NULL,
    io_root       VARCHAR   NOT NULL,
    state_root    VARCHAR   NOT NULL,
    messages_hash VARCHAR   NOT NULL,
    in_msgs_hash  VARCHAR   NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS runtime_round_uniq ON runtime_round (runtime_id, round);

CREATE TABLE IF NOT EXISTS runtime_transaction
(
    id             SERIAL PRIMARY KEY,
    runtime_id     VARCHAR   NOT NULL,
    round          INTEGER   NOT NULL,
    tx_hash        VARCHAR   NOT NULL,
    position       INTEGER   NOT NULL,
    evm_hash       VARCHAR   NOT NULL,
    consensus_from VARCHAR   NOT NULL,
    consensus_to   VARCHAR   NOT NULL,
    evm_from       VARCHAR   NOT NULL,
    evm_to         VARCHAR   NOT NULL,
    method         VARCHAR   NOT NULL,
    result         BOOLEAN   NOT NULL,
    messages       VARCHAR   NOT NULL,
    timestamp      TIMESTAMP NOT NULL,
    type           VARCHAR   NOT NULL,
    raw            JSON      NOT NULL,
    events         JSON      NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS runtime_transaction_uniq ON runtime_transaction (runtime_id, round, tx_hash);
CREATE INDEX IF NOT EXISTS runtime_transaction_tx_hash_idx ON runtime_transaction (tx_hash);
CREATE INDEX IF NOT EXISTS runtime_transaction_evm_hash_idx ON runtime_transaction (evm_hash);
CREATE INDEX IF NOT EXISTS runtime_transaction_consensus_from_idx ON runtime_transaction (consensus_from);
CREATE INDEX IF NOT EXISTS runtime_transaction_consensus_to_idx ON runtime_transaction (consensus_to);
CREATE INDEX IF NOT EXISTS runtime_transaction_evm_from_idx ON runtime_transaction (evm_from);
CREATE INDEX IF NOT EXISTS runtime_transaction_evm_to_idx ON runtime_transaction (evm_to);
CREATE INDEX IF NOT EXISTS runtime_transaction_method_idx ON runtime_transaction (method);

CREATE TABLE IF NOT EXISTS staking_event
(
    id         SERIAL PRIMARY KEY,
    height     INTEGER   NOT NULL,
    position   INTEGER   NOT NULL,
    tx_hash    VARCHAR   NOT NULL,
    kind       VARCHAR   NOT NULL,
    event_from VARCHAR   NOT NULL,
    event_to   VARCHAR   NOT NULL,
    amount     BIGINT    NOT NULL DEFAULT 0,
    raw        JSON      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS staking_event_uniq ON staking_event (height, position);
CREATE INDEX IF NOT EXISTS staking_event_tx_hash_idx ON staking_event (tx_hash);
CREATE INDEX IF NOT EXISTS staking_event_kind_idx ON staking_event (kind);
CREATE INDEX IF NOT EXISTS staking_event_consensus_from_idx ON staking_event (event_from);
CREATE INDEX IF NOT EXISTS staking_event_consensus_to_idx ON staking_event (event_to);

CREATE TABLE IF NOT EXISTS escrow_stats
(
    id             SERIAL PRIMARY KEY,
    height         INTEGER   NOT NULL,
    entity_address VARCHAR   NOT NULL,
    escrow         BIGINT    NOT NULL DEFAULT 0,
    date           TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS escrow_stats_uniq ON escrow_stats (entity_address, height);

CREATE TABLE IF NOT EXISTS runtime_node
(
    id         SERIAL PRIMARY KEY,
    runtime_id VARCHAR   NOT NULL,
    node_id    VARCHAR   NOT NULL,
    entity_id  VARCHAR   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS runtime_node_uniq ON runtime_node (runtime_id, node_id, entity_id);

CREATE TABLE IF NOT EXISTS runtime_stats
(
    id              SERIAL PRIMARY KEY,
    runtime_id      VARCHAR   NOT NULL,
    entity_id       VARCHAR   NOT NULL,
    rounds_elected  INTEGER   NOT NULL,
    rounds_primary  INTEGER   NOT NULL,
    rounds_backup   INTEGER   NOT NULL,
    rounds_proposer INTEGER   NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS runtime_stats_uniq ON runtime_stats (runtime_id, entity_id);

CREATE TABLE IF NOT EXISTS proposal
(
    id            SERIAL PRIMARY KEY,
    proposal_id   INTEGER   NOT NULL UNIQUE,
    title         VARCHAR   NOT NULL,
    type          VARCHAR   NOT NULL,
    submitter     VARCHAR   NOT NULL,
    state         VARCHAR   NOT NULL,
    created_epoch INTEGER   NOT NULL,
    closed_epoch  INTEGER   NOT NULL,
    raw           JSON      NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS vote
(
    id           SERIAL PRIMARY KEY,
    proposal_id  INTEGER   NOT NULL,
    vote_address VARCHAR   NOT NULL,
    option       VARCHAR   NOT NULL,
    amount       BIGINT    NOT NULL,
    percent      FLOAT     NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS vote_uniq ON vote (proposal_id, vote_address);