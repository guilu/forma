-- FOR-145c gap-table migration (1 of 5, ADR-012 design section 3 "5 GAP tables").
--
-- body_measurements (FOR-16, migration V2 + V6's muscle_mass_kg/water_percentage columns) had
-- ZERO owner-scoping at any layer before this slice -- BodyMeasurementRepository#list() returned
-- every account's rows to every caller. Its PK (id UUID) is a plain generated identity, not a
-- natural key shared across users, so no PK reconstruction is needed here: this is a simple
-- NET-NEW column, following V27's Class-A add-nullable -> backfill -> NOT NULL -> FK pattern
-- (contrast with V31/V34, which DO need the V28 shadow-table-swap because their existing PK is a
-- value that collides across users).
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).
ALTER TABLE body_measurements ADD COLUMN user_id UUID;
UPDATE body_measurements SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;
ALTER TABLE body_measurements ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE body_measurements
  ADD CONSTRAINT fk_body_measurements_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_body_measurements_user ON body_measurements (user_id);
