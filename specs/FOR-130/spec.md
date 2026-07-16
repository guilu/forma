# FOR-130 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-130
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-102 [STUB] Nutrition consumption logging + hydration (hydration slice).

## Summary

Hydration slice of FOR-102: persist water-intake entries and expose a hydration
progress read model (total logged volume vs daily goal). `POST /api/v1/nutrition/hydration`
and `GET /api/v1/nutrition/hydration?date=`. The daily goal is the user's
`DefaultObjectives.dailyWaterMl` (from the FOR-107 profile), with a documented fallback
default when unset. Independent of the nutrition-schedule gap — meal logging (FOR-127)
and consumed-vs-target (FOR-128) are already done.

## User/System Flow

1. User opens Nutrición (FOR-54) and taps "Añadir agua" → `POST /api/v1/nutrition/hydration` with a volume for the day.
2. Backend persists the entry and recomputes the day's total.
3. Frontend GETs `GET /api/v1/nutrition/hydration?date=` → total volume, daily goal, and progress.

## Functional Requirements

- Persist a `WaterIntakeEntry`: day, volume (ml), timestamp.
- A per-day hydration aggregate sums the day's entries.
- `POST /api/v1/nutrition/hydration` — log a positive volume for a day.
- `GET /api/v1/nutrition/hydration?date=YYYY-MM-DD` — total volume, daily goal, progress (`total/goal`).
- Resolve the daily goal from the user's `DefaultObjectives.dailyWaterMl` (via the FOR-107 profile). When unset, use a documented fallback default (e.g. 2000 ml). When the goal cannot be determined, progress is null (not fabricated).
- Owner-scoped (single-user MVP).

## Non-Functional Requirements

- **Security/Privacy**: intake volumes are personal health data — never log them at INFO; owner-scoped per ADR-002; do not bypass the boundary.
- **Performance**: per-day, low volume; a single query per day.
- **Correctness**: multiple entries in a day sum; never overwrite.

## Data Model Notes

- New domain: `WaterIntakeEntry` + per-day hydration aggregate. Implementer chooses aggregate shape (per-entry rows summed on read vs maintained total); document, and guarantee no drift from entries.
- Reuse: the FOR-107 profile (`UserProfileService`/`UserProfileRepository` → `DefaultObjectives.dailyWaterMl`) for the goal; do NOT duplicate profile logic.
- New migration: next free version is **V14** (current head V13). One column per statement (H2/PostgreSQL convention).

## Edge Cases

- Empty day (no entries) → `GET` returns 200 with total 0 (and goal/progress from the profile), never 404.
- Goal unset (`dailyWaterMl` null) → apply the documented fallback default; if progress cannot be defined, null — document.
- Multiple entries same day → summed.
- Negative or zero volume → 400 `VALIDATION_ERROR`.
- Missing/invalid/far-future date → 400.

## Open Questions

- Fallback daily-water default when `DefaultObjectives.dailyWaterMl` is null — pick a documented sensible value (e.g. 2000 ml) or expose progress as null; decide and document.
- Aggregate shape (per-entry rows vs maintained total) — pick the simplest consistent with FOR-127's meal-log persistence; document.
- Is edit/delete of a hydration entry in scope, or append-only for MVP? Default append-only unless trivial; document.
