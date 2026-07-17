# FOR-135 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-135
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (achievements slice).

## Summary

A closed, in-code catalog of achievement (logros) rules evaluated against existing data;
earned achievements are persisted (idempotent — awarded once, never un-earned or
duplicated). `GET /api/v1/progress/achievements` returns earned + available. Read-driven
evaluation persists newly-earned ones. No streaks (blocked by the documented training
per-date-history gap). See `specs/FOR-104/` for the full domain scope.

## User/System Flow

1. Frontend (Progreso, mockups 3/6) GETs `GET /api/v1/progress/achievements`.
2. Backend evaluates each catalog rule against the user's current data; any newly-met rule is awarded (persisted with earnedAt).
3. Response lists earned achievements (with earnedAt) and available (not-yet-earned) ones.

## Functional Requirements

- A closed `Achievement` catalog (in-code): each has a stable id, title/description, and a deterministic condition over EXISTING data. Implementer confirms each rule against the real repositories and keeps ONLY rules that real data supports. Suggested MVP set:
  - First body measurement; N measurements logged (e.g. 10).
  - First goal created; first goal achieved (FOR-125).
  - First meal logged; N meals logged (FOR-127).
  - First hydration entry; hit the daily hydration goal on a day (FOR-130).
  - First Withings sync completed (FOR-132) — only if cheaply derivable.
- Application service: evaluate rules against current data; persist newly-met ones; return earned + available.
- Idempotent award: a rule already earned is a no-op — never re-awarded or duplicated.
- Owner-scoped.

## Non-Functional Requirements

- **Explainable/auditable**: each award derives deterministically from source data (AGENTS quality bar).
- Reuse existing repositories (BodyMeasurement, Goals, MealLog, hydration) — do NOT duplicate their logic.
- Owner-scoped per ADR-002.

## Data Model Notes

- New table (migration **V18**, head V17): earned achievements keyed by `(owner_id, achievement_id)` with `earned_at`. PK enforces idempotency (never re-awarded).
- The catalog + rules are in-code (like `FoodCatalog`) — no catalog table.
- No streak/weekly-history state here.

## Edge Cases

- Empty data (nothing done yet) → no earned, full available list, never 404.
- Re-evaluating after already earning → no duplicate rows, earnedAt unchanged.
- A rule whose underlying data was later deleted → the earned achievement stays earned (persisted; achievements are not revoked) — document.
- Concurrent evaluation → PK prevents duplicates.

## Open Questions

- Exact MVP achievement set + thresholds — confirm each against real repositories; drop any rule whose data isn't cheaply available. Document the final set.
- Do count-based achievements (e.g. "10 meals") count all-time or within a window? Default all-time; document.
- Whether achievements are ever revoked if source data is deleted — default NO (persisted, motivating); document.
- Evaluation trigger: on GET (chosen for MVP) vs event-driven — document; on-GET is simplest.
