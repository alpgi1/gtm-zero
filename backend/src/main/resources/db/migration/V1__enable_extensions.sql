-- V1: Enable PostgreSQL extensions required by the GTM-Zero schema.
--
-- vector   → pgvector 0.8.2, provides the vector type and cosine/L2/IP operators
-- pgcrypto → provides gen_random_uuid() used as the default for UUID PKs

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
