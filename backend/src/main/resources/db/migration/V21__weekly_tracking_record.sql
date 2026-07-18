-- Weekly tracking record: weekly_tracking_record table (FOR-155, epic FOR-148 "Personalizar FORMA
-- a Diego", slice 7 of 7).
--
-- Persists one WeeklyTrackingRecord (the *Seguimiento* sheet row: Semana, Fecha, Peso kg, Grasa %,
-- Masa grasa kg, Masa magra kg, IMC, Km running, Ritmo 4 km, Kcal recomendadas, Comentario) per
-- week. Additive migration on top of V20 (ADR-003) — earlier migrations, including V2's
-- body_measurements and V20's user_profile columns, are untouched. A fresh CREATE TABLE may list
-- all columns in one statement (only a later ALTER ADD COLUMN needs one column per statement, H2
-- lesson, see ai-context.md).
--
-- Deliberately NOT a BodyMeasurement extension nor the WeeklyCheckIn insights snapshot (spec
-- FOR-155 Data Model Notes): this is a new, user-filled, persisted per-week aggregate.
--
-- Derived masses (fat_mass_kg, lean_mass_kg) are intentionally NOT stored, mirroring V2's
-- body_measurements: they are recomputed on read from weight_kg + body_fat_percentage via the
-- FOR-155 domain calculation, avoiding stored-vs-derived drift.
--
-- owner_id mirrors goal.owner_id/user_profile.owner_id/meal_log_entry.owner_id (V11/V20/V13):
-- account-scoped in shape even though authorization is not enforced yet for real multi-user auth
-- (ADR-002 single-user MVP).
--
-- Agreed model: SEGUIMIENTO starts EMPTY — no seed INSERT here. Only week 1 has real data in
-- docs/fitness_os.xlsm, and even that is the user's own data to enter through the API, not plan
-- seed data (spec FOR-155 Common Pitfalls: "Seeding weekly rows").
--
-- record_date (not "date") avoids ambiguity with the SQL DATE type name. pace_4km_min_per_km is a
-- short VARCHAR holding a validated "mm:ss" string (FOR-155 domain validation); it is never parsed
-- as a duration here, matching the sheet's plain-text "Ritmo 4 km" column.
--
-- One row per (owner_id, week): POST upserts by week (spec FOR-155 api.md "Create/upsert a weekly
-- record for a given week"), enforced by the unique index below.
CREATE TABLE weekly_tracking_record (
    id                   UUID PRIMARY KEY,
    owner_id             VARCHAR(64) NOT NULL,
    week                 INTEGER NOT NULL,
    record_date          DATE NOT NULL,
    weight_kg            NUMERIC(6, 3),
    body_fat_percentage  NUMERIC(5, 2),
    bmi                  NUMERIC(5, 2),
    running_km           NUMERIC(6, 2),
    pace_4km_min_per_km  VARCHAR(8),
    recommended_kcal     NUMERIC(6, 1),
    comment              TEXT
);

CREATE UNIQUE INDEX idx_weekly_tracking_record_owner_week
    ON weekly_tracking_record (owner_id, week);
