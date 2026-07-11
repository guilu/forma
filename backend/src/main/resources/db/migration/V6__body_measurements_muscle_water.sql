-- Body composition: add muscle_mass_kg and water_percentage to body_measurements (FOR-100).
--
-- Additive migration on top of V2 (ADR-003) — both columns are nullable so existing
-- rows keep loading unchanged (backward compatible). FOR-15 explicitly deferred these
-- two fields; they are now backed to support the FOR-52 "AGUA CORPORAL" card.
--
-- muscle_mass_kg is a directly MEASURED value (e.g. from a smart scale), distinct from
-- the DERIVED lean_mass_kg computed on read from weight_kg + body_fat_percentage; it is
-- not conflated with it and not recomputed here.
ALTER TABLE body_measurements ADD COLUMN muscle_mass_kg NUMERIC(6, 3);
ALTER TABLE body_measurements ADD COLUMN water_percentage NUMERIC(5, 2);
