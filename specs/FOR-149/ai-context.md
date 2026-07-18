# FOR-149 AI Context

## Story

FOR-149 — Perfil: targets personales + baseline de Diego. Slice 1 of epic FOR-148 (personalize
FORMA to Diego's real plan). Seeds plan (fijo) profile data; measurements stay empty.

## Intent

Persist the *Perfil* sheet targets and seed Diego's baseline so the dashboard/insights show his real
numbers instead of empty/demo defaults. Success = profile carries base kcal, fat/weight target
ranges, and protein/fat/carb targets; Diego's row is seeded; dashboard reads the real targets.

## Relevant Documents

- `AGENTS.md` — hexagonal, domain owns rules, no user data hardcoded outside seed/fixtures.
- `docs/fitness_os.xlsm` — sheet **Perfil** (targets + baseline) and **Dashboard** (recommended-kcal / lean-mass / BMI target bands). Source of truth.
- `docs/adr/ADR-001-architecture.md` (domain owns rules, none in UI), `ADR-002-authentication.md` (owner-scoping, fixed OWNER_ID), `ADR-003-persistence.md` (additive migrations).
- Jira: https://dbhlab.atlassian.net/browse/FOR-149

## Domain / Repo Notes (verified)

- `UserProfile` record: profile fields nullable; `defaults(ownerId)` returns empty objectives, dark theme.
- `DefaultObjectives(caloricDeficitKcal, proteinTargetG, dailyWaterMl)` — non-negative when present; `EMPTY` all null. This is the extension point (or a sibling `PersonalTargets`).
- `V8__user_profile.sql`: one combined `user_profile` table, `owner_id` PK, objective columns limited to deficit/protein/water.
- Migration head **V19**. Next slice migration claims the next free `V<N>` at implementation time.

## Architectural Constraints

- Additive migration (ADR-003): `ALTER TABLE user_profile ADD COLUMN ...`, one column per statement, nullable (or NOT NULL DEFAULT matching domain defaults where sensible). Never edit V8.
- Framework-free domain (ADR-001). Owner-scoped (ADR-002); seed under the fixed OWNER_ID.
- Seed is reference data (plan). Do NOT seed `body_measurement` rows — SEGUIMIENTO starts empty.

## Common Pitfalls

- Seeding baseline weight/fat as a `body_measurement` row — that's slice 7 / SEGUIMIENTO; keep the table empty.
- Hard-coding the migration version — compute the next free `V<N>` against the current head (V19).
- Putting multiple columns in one ALTER statement — one column per statement (repo convention).
- Overwriting `caloric_deficit_kcal` semantics with base kcal — base kcal is a new field; deficit stays distinct.
- Recomputing targets in the frontend — the UI renders backend read models (ADR-001/ADR-006).

## Suggested Implementation Order

1. Extend the domain value object with the new target fields (+ validation, nullable), tested.
2. Additive migration adding the target columns (next free `V<N>`, one column per statement).
3. Persistence adapter + mapping for the new columns; seed Diego's profile as reference data.
4. Expose targets in the profile read model the dashboard/insights consume.

## Validation

Backend build + tests (`./gradlew build`). Confirm: new targets persist/read back; Diego's seed matches the *Perfil* sheet exactly; migration is additive and claims a fresh `V<N>` above V19 with one column per statement; unseeded profile still valid (no 404); `body_measurement` still empty; no domain logic leaked to the UI.
