-- V1__initial_schema.sql
-- Multi-Agent Research System — Initial DB Schema

-- ── Extensions ───────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── research_jobs ────────────────────────────────────────────────────────
CREATE TABLE research_jobs (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    query         TEXT         NOT NULL,
    query_hash    VARCHAR(64)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    elapsed_ms    BIGINT,
    error_message TEXT,
    search_top_n      INT          DEFAULT 10,
    reranker_top_k    INT          DEFAULT 5,
    refinement_iterations INT      DEFAULT 2,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_research_jobs_user_id    ON research_jobs(user_id);
CREATE INDEX idx_research_jobs_status     ON research_jobs(status);
CREATE INDEX idx_research_jobs_created_at ON research_jobs(created_at DESC);
CREATE INDEX idx_research_jobs_query_hash ON research_jobs(query_hash);

-- ── research_results ─────────────────────────────────────────────────────
CREATE TABLE research_results (
    id                        UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    job_id                    UUID        NOT NULL UNIQUE REFERENCES research_jobs(id) ON DELETE CASCADE,
    answer                    TEXT        NOT NULL,
    sources                   JSONB,
    confidence                DOUBLE PRECISION,
    critic_feedback           JSONB,
    refinement_iterations_run INT,
    elapsed_seconds           DOUBLE PRECISION,
    pipeline_errors           JSONB,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                   BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_research_results_job_id ON research_results(job_id);

-- ── audit_logs ───────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id        VARCHAR(255) NOT NULL,
    action         VARCHAR(100) NOT NULL,
    resource       VARCHAR(100),
    resource_id    VARCHAR(100),
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    correlation_id VARCHAR(64),
    latency_ms     BIGINT,
    success        BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ── update trigger ───────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_research_jobs_updated
    BEFORE UPDATE ON research_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_research_results_updated
    BEFORE UPDATE ON research_results
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_audit_logs_updated
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
