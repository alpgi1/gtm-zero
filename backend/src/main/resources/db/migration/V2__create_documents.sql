-- V2: documents + document_chunks tables.
--
-- documents       → uploaded source material (READMEs, API specs, architecture docs)
-- document_chunks → chunked + embedded segments used by the RAG retrieval layer
--
-- The set_updated_at() trigger function is created once here (CREATE OR REPLACE)
-- and reused by V3 and later migrations.

-- ────────────────────────────────────────────────────────────
-- Reusable trigger function: keeps updated_at in sync on every UPDATE
-- ────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ────────────────────────────────────────────────────────────
-- Table: documents
-- ────────────────────────────────────────────────────────────
CREATE TABLE documents (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT        NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_path TEXT,
    raw_content TEXT        NOT NULL,
    char_count  INTEGER     NOT NULL,
    chunk_count INTEGER     NOT NULL DEFAULT 0,
    ingested_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_documents_source_type CHECK (
        source_type IN ('README', 'TECHNICAL_DOC', 'API_SPEC', 'ARCHITECTURE', 'LEGAL_CORPUS')
    )
);

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ────────────────────────────────────────────────────────────
-- Table: document_chunks
-- ────────────────────────────────────────────────────────────
CREATE TABLE document_chunks (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    token_count INTEGER,
    embedding   VECTOR(1024) NOT NULL,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_document_chunks_doc_index UNIQUE (document_id, chunk_index)
);

-- IVFFlat index: fast approximate cosine similarity search.
-- lists=100 is appropriate for ~50-200 chunks; revisit if corpus grows past 10k.
CREATE INDEX idx_document_chunks_embedding
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_document_chunks_document_id ON document_chunks (document_id);
