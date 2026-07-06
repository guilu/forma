-- Body composition: body_measurements table (FOR-16).
--
-- Persists one BodyMeasurement (FOR-15) per row. Additive migration on top of
-- the V1 baseline (ADR-003) — V1 stays untouched.
--
-- Derived masses (fat_mass_kg, lean_mass_kg) are intentionally NOT stored: they
-- are recomputed on read from weight_kg + body_fat_percentage via the FOR-15
-- domain calculation, avoiding stored-vs-derived drift.
--
-- No provider-specific columns (Withings tokens, sync metadata) live here;
-- integration data stays in its own adapters (docs/domain-model.md). `source`
-- is a short text column so a future external source (e.g. WITHINGS) fits
-- without a schema rewrite.
CREATE TABLE body_measurements (
    id                  UUID PRIMARY KEY,
    measured_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    source              VARCHAR(32) NOT NULL,
    weight_kg           NUMERIC(6, 3) NOT NULL,
    body_fat_percentage NUMERIC(5, 2),
    bmi                 NUMERIC(5, 2),
    notes               TEXT
);

-- Listing defaults to most-recent-first (FOR-16 list() contract), so index the
-- ordering column descending.
CREATE INDEX idx_body_measurements_measured_at ON body_measurements (measured_at DESC);
