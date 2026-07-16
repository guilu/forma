# FOR-129 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-129
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (slice 2 of 6).

## Summary

Second implementable slice of FOR-104: an **adherence read model** exposing planned
vs completed per category (training, nutrition, measurements) over a rolling window
(default 30 days), derived from existing data. `GET /api/v1/progress/adherence?days=30`.
Read-only derivation — no new persistence. Streaks, achievements, photos and the
muscle-worked map are later FOR-104 slices. See `specs/FOR-104/` for the full scope.

## User/System Flow

1. Frontend (Progreso, mockups 3/6 / FOR-56 context) GETs `GET /api/v1/progress/adherence?days=30`.
2. Backend computes, per category, planned vs completed counts over the window and a rate.
3. Response lists categories (TRAINING, NUTRITION, MEASUREMENTS) with `planned`, `completed`, `rate`.

## Functional Requirements

- Compute adherence over a rolling window ending today, `days` configurable (default 30).
- **TRAINING**: planned sessions from the weekly training schedule over the window vs sessions marked COMPLETED. Reuse `WeeklyTrainingScheduleService` / `TrainingSessionStatusService` / the shared `WeeklyTrainingDayPolicy` (FOR-128) — do NOT duplicate the counting/policy.
- **NUTRITION**: derived from meal logs (FOR-127). Document the exact definition — e.g. days with at least one logged entry vs days in the window (a "logged consistently" measure), since there is no per-day "planned meals" count today.
- **MEASUREMENTS**: actual `BodyMeasurement` entries in the window vs an expected cadence (document the assumed cadence, e.g. weekly → `ceil(days/7)` expected).
- Owner-scoped (single-user MVP).

## Non-Functional Requirements

- **No new persistence/migration** — pure derivation over existing repositories.
- **No duplicated counting/policy** — reuse training schedule/status, meal-log repo, measurement repo.
- **Explainability**: each category's planned/completed must be auditable from source data.
- Owner-scoped per ADR-002.

## Data Model Notes

- New: an `Adherence` read model (per-category planned/completed/rate) + an application service; no domain aggregate to persist.
- Reuse: `WeeklyTrainingScheduleService`, `TrainingSessionStatusService`, `WeeklyTrainingDayPolicy` (FOR-128), `MealLogRepository` (FOR-127), `BodyMeasurementRepository`.
- No new tables, no migration (head stays V13).
- `rate = completed / planned`; when `planned == 0`, `rate` is null (or 0) — document; never divide by zero.

## Edge Cases

- `days` out of a bounded range (e.g. 1–365) → 400 `VALIDATION_ERROR`.
- Zero planned in a category (e.g. no scheduled training in window) → planned 0, rate null/0, not an error.
- Empty data (no sessions/logs/measurements) → 200 with zeroed categories, never 404.
- Window spanning today: define inclusivity of the endpoints and document (e.g. `[today-days+1, today]`).
- `completed > planned` (e.g. extra logged measurements beyond cadence) → allowed; cap rate at 1.0 or report raw — document.

## Open Questions

- NUTRITION "planned" definition — days-with-log vs a future per-day planned-meals count. Use days-with-log for MVP; document.
- MEASUREMENTS expected cadence — weekly by default? Confirm against product intent; document the assumption.
- `rate` when planned is 0: null vs 0 — pick one and be consistent across categories.
- Whether to cap `rate` at 1.0 when completed exceeds planned.
