# FOR-130 AI Context

## Story

FOR-130 — Water-intake logging and hydration progress read model. Hydration slice of FOR-102 [STUB] Nutrition consumption logging + hydration.

## Intent

Add real hydration tracking: log water intake and expose total vs daily goal + progress. Success = FOR-54 "Añadir agua" has a real backend. Independent of the nutrition-schedule gap; meal logging (FOR-127) and consumed-vs-target (FOR-128) are already done.

## Relevant Documents

- `specs/FOR-102/` — full nutrition-consumption scope and slicing.
- `specs/FOR-127/` — the meal-logging slice (same `delivery/nutrition` area; persistence pattern reference).
- `AGENTS.md` — hexagonal boundaries, owner-scoping, never log sensitive health data.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-130

## Domain Notes

- New `WaterIntakeEntry` + per-day hydration aggregate — no such concept exists today.
- The daily goal is the user's `DefaultObjectives.dailyWaterMl` (a nullable Double in ml), reached via the FOR-107 profile (`UserProfileService`/`UserProfileRepository`, `UserProfile.defaultObjectives`). Reuse it; do NOT duplicate profile logic. Fallback to a documented default when null.
- Mirror FOR-127's meal-log persistence style (per-entry rows, derived-on-read totals) unless a maintained total is clearly simpler — document and guarantee no drift.

## Architectural Constraints

- Domain framework-free; application port + service; thin controller under `delivery/nutrition` (same area as FOR-127); JDBC adapter under `adapter/persistence`.
- Reuse the profile service for the goal — avoid a circular dependency; the hydration service can depend on the profile service (one direction).
- New migration is **V14** (current head V13); one column per statement.
- Owner-scoped (ADR-002); never log intake volumes at INFO.

## Common Pitfalls

- Overwriting the day's total instead of summing entries.
- Duplicating profile/objectives logic instead of reading `DefaultObjectives.dailyWaterMl` via the profile service.
- Fabricating progress when the goal is unknown — use the documented fallback or null progress.
- Returning 404 for an empty day instead of total 0.
- Logging intake volumes (personal health data).
- Building meal logging / key nutrients here — those are other slices.

## Suggested Implementation Order

1. `WaterIntakeEntry` + per-day hydration aggregate domain (+ tests): summing, positive-volume validation, progress vs goal.
2. Application port + service (log entry, hydration read model reading the goal from the profile), owner-scoped.
3. JDBC adapter + `V14` migration (+ persistence round-trip test).
4. `delivery/nutrition` endpoints `POST /nutrition/hydration` + `GET /nutrition/hydration` + DTOs (+ API tests) per `api.md`.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: logging sums the day's total; goal read from `DefaultObjectives.dailyWaterMl` with a documented fallback; progress null only when goal undeterminable; empty day → total 0 (not 404); volume <= 0 / bad date → 400; volumes never logged. Then FOR-54 (frontend) can consume the endpoints.
