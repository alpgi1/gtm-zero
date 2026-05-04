-- V5: audit_log table.
--
-- Lightweight event log that powers the dashboard "recent activity" feed.
-- Indexed on (event_type, created_at DESC) for the two most common query patterns.

CREATE TABLE audit_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    entity_id   UUID,
    summary     TEXT         NOT NULL,
    metadata    JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_event_type_created_at
    ON audit_log (event_type, created_at DESC);

CREATE INDEX idx_audit_log_created_at
    ON audit_log (created_at DESC);
