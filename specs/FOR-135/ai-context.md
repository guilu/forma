# FOR-135 AI Context

## Story

FOR-135 — Achievements: rule-driven, persisted logros. Achievements slice of FOR-104 [STUB] Progress & goals domain.

## Intent

Add a motivating, explainable achievements signal: a closed in-code catalog of deterministic rules over existing data, awarded once and persisted. Success = `GET /api/v1/progress/achievements` returns earned + available logros. No streaks (blocked by the training per-date-history gap).

## Relevant Documents

- `specs/FOR-104/` — full progress/goals scope; `specs/FOR-129/` (adherence, sibling slice).
- `AGENTS.md` — hexagonal, explainable, no duplicated logic, owner-scoping.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`.
- Mockups: `docs/3-entrenamiento.png`, `docs/6-progreso.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-135

## Domain / Repo Notes

- Reuse existing repositories for rule evaluation: `BodyMeasurementRepository`, Goals (FOR-125 `GoalRepository`/service), `MealLogRepository` (FOR-127), hydration (FOR-130 `WaterIntakeRepository`). Verify each repo's real query methods before writing a rule; if a rule's data isn't cheaply available, drop that rule.
- Adherence read model (FOR-129) exists but relies on the approximate training projection — do NOT base an achievement on per-date training completion (documented gap).
- The catalog + rules are IN-CODE (like `FoodCatalog`) — no catalog table. Only earned achievements are persisted.

## Architectural Constraints

- Domain: an `Achievement` catalog (closed set) + deterministic rule conditions; framework-free. Application service evaluates + awards. JDBC adapter persists earned. Thin `delivery/progress` controller.
- Idempotent award: PK `(owner_id, achievement_id)`; already-earned = no-op. Achievements are NOT revoked if data is later deleted (persisted; document).
- New migration is **V18** (current head V17); one column per statement.
- Owner-scoped (ADR-002); explainable from source data.
- Do NOT duplicate repository query logic.

## Common Pitfalls

- Basing an achievement on per-date training completion history (the documented gap) — the catalog must contain none.
- Re-awarding / duplicating an earned achievement — enforce via PK + no-op on re-evaluate.
- Fabricating a rule whose data isn't actually available — verify against the real repos, drop unsupported rules.
- Returning 404 on empty data instead of empty earned + full available.
- Duplicating existing repository counting logic instead of reusing it.
- Adding a catalog table (rules are in-code).

## Suggested Implementation Order

1. `Achievement` catalog (closed set) + rule conditions (pure, tested with fixtures); confirm each rule against a real repository, drop unsupported ones.
2. Application service: evaluate → award newly-met → return earned + available (owner-scoped), idempotent.
3. Earned-achievement JDBC store + `V18` migration (PK owner_id+achievement_id) (+ round-trip + duplicate-prevention tests).
4. `delivery/progress` `GET /api/v1/progress/achievements` + DTO (+ API tests).

## Validation

Run backend build + tests (`./gradlew build`). Confirm: rules fire deterministically at their thresholds; newly-met rules are awarded + persisted; re-evaluation never duplicates (PK); earned carry earnedAt; empty data → empty earned + full available (not 404); no streak/per-date-training rule; migration V18 unique; owner-scoped.
