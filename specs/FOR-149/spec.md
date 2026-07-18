# FOR-149 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-149
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheet **Perfil** (and Dashboard targets).
Slice 1 of 7. Recommended after slice 3 (FOR-151, days). Base for slice 2 insights (FOR-150) and dashboard.

## Summary

Add the personal targets from the Excel *Perfil* sheet to the profile domain and seed Diego's
baseline as plan reference data. Today `UserProfile`/`DefaultObjectives` persist only 3 objective
fields (caloric deficit, protein, water) and the `user_profile` table ships empty. This slice adds
the missing target fields, an additive migration, and seeds Diego's real profile.

This is **plan (fijo)** data per the epic model: profile targets + baseline are seeded via
reference data. Body measurements (SEGUIMIENTO) stay empty — they belong to slice 7 (FOR-155).

## Excel target data (sheet Perfil, verified)

- Nombre **Diego**, Edad **45**, Altura **1.80 m**.
- Baseline: Peso inicial **73.6 kg**, Grasa inicial **14.7 %**, IMC inicial **22.7**.
- Objetivo grasa **13 %** (rango **12–13 %**).
- Objetivo peso **73–75 kg** (recomposición, no pérdida agresiva).
- Kcal base **2300 kcal/día** (ajuste semanal según tendencia).
- Proteína **160 g/día**, Grasas **70 g/día**, Carbohidratos **260 g/día**.
- Dashboard companions: Kcal recomendadas objetivo **2200–2400**, Masa magra objetivo **>62 kg**, IMC objetivo **22–23**.

## Current repository state (verified)

- `domain/UserProfile.java` — record; profile fields (name, birthDate, sex, heightCm) exist and are nullable; carries a `DefaultObjectives`.
- `domain/DefaultObjectives.java` — record `(caloricDeficitKcal, proteinTargetG, dailyWaterMl)`; `EMPTY = (null,null,null)`. **No base kcal, no fat/carb macros, no target ranges.**
- `resources/db/migration/V8__user_profile.sql` — columns `caloric_deficit_kcal`, `protein_target_g`, `daily_water_ml`; no base-kcal, no range columns. Table seeds no rows (fresh install returns `UserProfile.defaults(ownerId)`).
- Migration head is **V19** (`V19__progress_photo.sql`).

## Functional Requirements

- Extend the objectives value object (`DefaultObjectives` or a dedicated `PersonalTargets`) with: base kcal, body-fat target range (min/max %), weight target range (min/max kg), protein/fat/carb targets. Reuse the existing `protein_target_g` column for protein (160). Keep every target optional/nullable so an unseeded profile is still valid.
- Additive migration (next free `V<N>` at implementation time — do NOT hard-code; head is V19, one column per `ALTER TABLE ... ADD COLUMN` statement, ADR-003) adding the new target columns to `user_profile`.
- Seed Diego's profile as plan reference data: name, height, baseline (weight/fat/BMI reference values on profile), and all targets. Fixed `OWNER_ID` (ADR-002 single-user MVP).
- Expose the new targets where the dashboard/insights consume them (recommended kcal, fat/weight target range) — read model only; no domain logic in the UI (ADR-001).

## Non-Functional Requirements

- Additive migration only (ADR-003) — earlier migrations untouched; one column per statement.
- Owner-scoped in shape (ADR-002); framework-free domain (ADR-001).
- Explainable: targets sourced verbatim from the *Perfil* sheet; never fabricated.

## Data Model Notes

- Baseline values (peso/grasa/IMC iniciales) are **profile reference fields**, not `body_measurement`
  rows — the measurements table stays empty (SEGUIMIENTO, slice 7). Document this split to avoid
  double-seeding.
- Decide range representation: two nullable columns per range (`body_fat_target_min_pct`/`_max_pct`,
  `weight_target_min_kg`/`_max_kg`) is the simplest additive shape.
- "Kcal recomendadas 2200–2400" (Dashboard) may be **derived** from base kcal ±band or stored — pick
  in design and document; base kcal 2300 sits inside that band.

## Edge Cases

- Fresh profile (no seed / other owner) → all new targets null; `defaults(ownerId)` still valid, no 404.
- Partial targets set → the profile still reads back consistently (nullable columns).

## Open Questions

- New value object vs extending `DefaultObjectives` (naming: these are targets, `DefaultObjectives` today is deficit/protein/water presets).
- Recommended-kcal band: derived from base kcal or stored explicitly.
- Whether baseline BMI (22.7) is stored or derived from weight+height.
