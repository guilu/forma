-- Withings measures sync (FOR-132, slice 3 of FOR-103, on top of the FOR-131 OAuth/token layer).
-- Additive on top of V15 (ADR-003) — earlier migrations are untouched.

-- Idempotent duplicate-detection markers (ADR-004: "Synchronization must be idempotent. Duplicate
-- detection is mandatory for imported records."). One row per imported Withings measure group,
-- keyed by (owner_id, provider, grpid) — re-syncing the same Withings history skips every grpid
-- already present here instead of creating duplicate body_measurements rows. Deliberately a
-- separate table, not a column on body_measurements: BodyMeasurement stays provider-clean by design
-- (spec FOR-132 Repository baseline: "do NOT add an external id") — the dedup key lives entirely on
-- the Integrations side, mirroring integration_token (V15) living apart from integration_connection.
CREATE TABLE integration_measure_marker (
    owner_id    VARCHAR(64) NOT NULL,
    provider    VARCHAR(32) NOT NULL,
    grpid       BIGINT NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (owner_id, provider, grpid)
);

-- Extends integration_connection (V12) with the FOR-132 sync outcome's new duplicatesSkipped count,
-- alongside the existing last_sync_result/last_sync_imported_count/last_sync_message columns it was
-- already flattened onto — not a new table, since there is still at most one "last" outcome per
-- connection (spec FOR-126 Data Model Notes, unchanged by this story). One column per statement,
-- matching V12/V15's convention (H2/PostgreSQL compatibility).
ALTER TABLE integration_connection ADD COLUMN last_sync_duplicates_skipped INTEGER;
