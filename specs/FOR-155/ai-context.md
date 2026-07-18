# FOR-155 AI Context

## Story

FOR-155 — Check-in semanal: km/ritmo/kcal recomendadas/comentario. Slice 7 of epic FOR-148. Adds a
persisted weekly tracking record (SEGUIMIENTO, starts empty). Enables FOR-150 rules 4–5.

## Intent

Capture the whole *Seguimiento* row per week so the user logs km, pace, recommended kcal and a
comment alongside body metrics. Success = a weekly record exists, persists, is exposed via API,
starts empty, and feeds the FOR-150 pace/hunger/trend rules.

## Relevant Documents

- `AGENTS.md` — hexagonal, domain-first, no fabricated data; SEGUIMIENTO starts empty.
- `docs/fitness_os.xlsm` — sheet **Seguimiento** (one row per week, 11 columns). Source of truth.
- `docs/adr/ADR-001-architecture.md` (framework-free domain), `ADR-002-authentication.md` (owner-scoping, fixed OWNER_ID), `ADR-003-persistence.md` (additive migration).
- Siblings: `specs/FOR-149/` (profile base kcal → recommended kcal), `specs/FOR-150/` (rules 4–5 consume pace/hunger/HR).
- Jira: https://dbhlab.atlassian.net/browse/FOR-155

## Domain / Repo Notes (verified)

- `BodyMeasurement` — per-event, no km/ritmo/kcal, no week concept; validates positive weight and % bounds; derives fat/lean mass.
- `WeeklyCheckIn` — insights snapshot (not persisted as a user row): week start + latest body values + session counts.
- `V2__body_measurements.sql` — per-event table. Migration head **V19**.
- `frontend/src/pages/MeasurementsPage.tsx` — measurements UI consumer (out of this slice's write scope).

## Architectural Constraints

- New `WeeklyTrackingRecord` aggregate + table recommended (don't overload BodyMeasurement/WeeklyCheckIn).
- Additive migration, next free `V<N>` above V19 (ADR-003). A fresh `CREATE TABLE` may list all columns; any later `ALTER ADD COLUMN` must be one column per statement (H2 lesson).
- Framework-free domain (ADR-001), owner-scoped (ADR-002). Starts empty — no seed rows.
- Values validated for consistency (BodyMeasurement precedent).

## Common Pitfalls

- Seeding weekly rows — SEGUIMIENTO starts empty; only week-1 exists in the Excel and even that is the user's data, not plan seed.
- Overloading `BodyMeasurement` (per-event) or `WeeklyCheckIn` (insights snapshot) instead of a new per-week record.
- Fabricating kcal recomendadas — derive from FOR-149 base kcal or take user input; document which.
- Assuming rule 4 is fully enabled — the sheet has ritmo but not FC/ppm; flag the HR gap for FOR-150.
- Hard-coding the migration version instead of the next free `V<N>`.

## Suggested Implementation Order

1. Model `WeeklyTrackingRecord` (week key + body metrics + km + ritmo + kcal recomendadas + comentario) with validation, tested.
2. Additive migration creating the weekly table (next free `V<N>`).
3. Persistence adapter + API (create/list/read); empty-list behavior.
4. Decide kcal-recomendadas source (derived vs entered) and the optional HR field; wire fields for FOR-150.

## Validation

Backend build + tests (`./gradlew build`). Confirm: weekly record persists/reads back all *Seguimiento* fields; collection starts empty (200 + empty state, no seed); partial records valid; migration additive above V19; kcal-recomendadas source documented; fields available to FOR-150 rules 4–5; owner-scoped.
