-- V4: objection_queries table.
--
-- Every technical objection asked during a demo session is logged here.
-- retrieved_chunk_ids records which RAG chunks fed the answer (full audit trail).

CREATE TABLE objection_queries (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id            UUID        NOT NULL,
    question              TEXT        NOT NULL,
    answer                TEXT        NOT NULL,
    retrieved_chunk_ids   UUID[]      NOT NULL,
    citation_count        INTEGER     NOT NULL,
    model                 VARCHAR(100),
    first_token_latency_ms INTEGER,
    total_latency_ms      INTEGER,
    token_count_input     INTEGER,
    token_count_output    INTEGER,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_objection_queries_session_id  ON objection_queries (session_id);
CREATE INDEX idx_objection_queries_created_at  ON objection_queries (created_at DESC);
