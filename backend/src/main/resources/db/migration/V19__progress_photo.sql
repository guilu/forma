-- Progress photo metadata (FOR-140, progress-photos slice / slice 6 of FOR-104).
--
-- PRIVACY-SENSITIVE: this table holds METADATA ONLY. The binary itself is never stored here (or
-- anywhere in PostgreSQL) — it lives behind the ProgressPhotoStore port (a private filesystem
-- adapter for this MVP, spec FOR-140 Storage decision, Option A), keyed by storage_ref. There is
-- deliberately no URL column: retrieval always goes through the owner-scoped, access-controlled
-- GET /api/v1/progress/photos/{id} endpoint, never a public/static/durable link (spec FOR-140 api.md).
--
-- Additive on top of V18 (ADR-003) — earlier migrations are untouched. This is the first
-- binary-storage-adjacent migration in the codebase (spec FOR-140: "first binary-storage concern").
--
-- owner_id mirrors goal.owner_id/earned_achievement.owner_id (V11/V18): account-scoped in shape
-- even though authorization is not enforced yet for real multi-user auth (ADR-002 single-user MVP)
-- — the owner boundary is nonetheless enforced in the application layer (ProgressPhotoService),
-- never bypassed just because today there is only one real account.
CREATE TABLE progress_photo (
    id           UUID PRIMARY KEY,
    owner_id     VARCHAR(64) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    storage_ref  VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_progress_photo_owner ON progress_photo (owner_id);
