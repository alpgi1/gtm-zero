-- V3: prospects + outreach_messages tables.
--
-- prospects         → target companies/contacts for the outreach feature
-- outreach_messages → AI-generated cold outreach emails (multiple per prospect = history)

-- ────────────────────────────────────────────────────────────
-- Table: prospects
-- ────────────────────────────────────────────────────────────
CREATE TABLE prospects (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name           VARCHAR(255),
    role                VARCHAR(255),
    company_name        VARCHAR(255) NOT NULL,
    company_domain      VARCHAR(255),
    linkedin_url        TEXT,
    github_url          TEXT,
    tech_stack_signals  TEXT[],
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_prospects_updated_at
    BEFORE UPDATE ON prospects
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_prospects_company_name    ON prospects (company_name);
CREATE INDEX idx_prospects_linkedin_url    ON prospects (linkedin_url) WHERE linkedin_url IS NOT NULL;
CREATE INDEX idx_prospects_github_url      ON prospects (github_url)   WHERE github_url   IS NOT NULL;

-- ────────────────────────────────────────────────────────────
-- Table: outreach_messages
-- ────────────────────────────────────────────────────────────
CREATE TABLE outreach_messages (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    prospect_id                 UUID         NOT NULL REFERENCES prospects(id) ON DELETE CASCADE,
    subject                     VARCHAR(500),
    body                        TEXT         NOT NULL,
    generation_model            VARCHAR(100),
    generation_prompt_version   VARCHAR(50),
    generation_latency_ms       INTEGER,
    status                      VARCHAR(50)  NOT NULL DEFAULT 'GENERATED',
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_outreach_status CHECK (
        status IN ('GENERATED', 'APPROVED', 'SENT_MOCK')
    )
);

CREATE TRIGGER trg_outreach_messages_updated_at
    BEFORE UPDATE ON outreach_messages
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_outreach_messages_prospect_id ON outreach_messages (prospect_id);
CREATE INDEX idx_outreach_messages_created_at  ON outreach_messages (created_at DESC);
