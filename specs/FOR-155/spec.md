# FOR-155 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-155
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheet **Seguimiento** (one row per week).
Slice 7 of 7. Enables slice 2 rules 4–5 (FOR-150). Recommended early (after slice 3/1).

## Summary

Capture the full *Seguimiento* sheet as a **weekly tracking record** (one row per week) rather than
only per-event body metrics. Adds km running, ritmo (4 km), kcal recomendadas and comentario next to
the body-composition values. This is **SEGUIMIENTO (variable)**: it starts **empty** and the user
fills it weekly — no seed rows.

## Excel columns (sheet Seguimiento — one row per week, verified)

Semana · Fecha · Peso kg · Grasa % · Masa grasa kg · Masa magra kg · IMC · **Km running** · **Ritmo 4 km** · **Kcal recomendadas** · **Comentario**

24 weeks planned, all empty except week 1 (agreed model: seguimiento starts empty).

## Current repository state (verified)

- `domain/BodyMeasurement.java` — **per-event** value: `measuredAt, source, weightKg, bodyFatPercentage, bmi, muscleMassKg, waterPercentage, notes` (+ derived fat/lean mass). **No km, no ritmo, no kcal recomendadas; no "week N" concept.**
- `domain/WeeklyCheckIn.java` — an **insights snapshot** (not a persisted user row): `weekStartDate` + latest body values + planned/completed session counts + notes. No km/ritmo/kcal.
- `resources/db/migration/V2__body_measurements.sql` — the per-event measurements table.
- `frontend/src/pages/MeasurementsPage.tsx` — consumes measurements (UI consumer; not in this slice's write scope).
- Migration head **V19**.

## Functional Requirements

- Model a **weekly tracking record** (e.g. `WeeklyTrackingRecord`) keyed by week (Semana + Fecha) capturing: peso, grasa %, masa grasa, masa magra, IMC, **km running**, **ritmo 4 km**, **kcal recomendadas**, **comentario**.
- Persist it via an additive migration (next free `V<N>` at implementation time — do NOT hard-code; head is V19; ADR-003; H2 rejects multi-column ALTER, but a fresh `CREATE TABLE` with all columns is fine).
- API: create/list/read weekly records; the collection **starts empty** (no seed rows) with a proper empty state.
- These fields feed FOR-150: rule 4 (running HR/pace) and rule 5 (hunger) — plus trend rules. See the cross-slice note below on which fields those rules need.

## Non-Functional Requirements

- Framework-free domain (ADR-001); owner-scoped (ADR-002, fixed OWNER_ID); additive migration (ADR-003).
- Values validated for internal consistency (following the `BodyMeasurement` precedent: positive weight, % bounds).
- Explainable: fields map 1:1 to the *Seguimiento* columns; never fabricated. Empty is a valid state.

## Data Model Notes

- **New entity vs extending existing**: `BodyMeasurement` is per-event and `WeeklyCheckIn` is an
  insights snapshot — neither is a persisted per-week user row. Recommend a **new** `WeeklyTrackingRecord`
  aggregate + table rather than overloading either; document the decision. Body values may be entered
  or derived from a linked measurement — decide and document.
- **Kcal recomendadas**: the sheet asks for it but it can be **derived** from the profile base kcal
  (FOR-149) or **entered** by the user — decide in design (the "Nota" in the story flags this).
- **Ritmo 4 km** is user-entered.
- **Cross-slice gap (flag)**: FOR-150 rule 4 is "same pace, more **ppm**" — it needs a **heart-rate**
  signal, but the *Seguimiento* sheet lists ritmo, **not FC/ppm**. Either add an optional HR field to
  the weekly record or keep rule 4 gated / pace-degradation-based. Resolve with FOR-150 in design.

## Edge Cases

- No records yet → empty list, 200, empty state (not an error). This is the default (seguimiento empty).
- A week with only some fields filled (e.g. body metrics but no km) → partial record is valid.
- Week uniqueness: one record per Semana N (decide dedupe/upsert behavior).

## Open Questions

- New `WeeklyTrackingRecord` table vs extending measurements — recommend new; confirm.
- Kcal recomendadas derived (from FOR-149) vs entered.
- Whether to add an optional heart-rate field now to fully enable FOR-150 rule 4.
- Keying by week number vs week-start date (align with `WeeklyCheckIn.weekStartDate`).
