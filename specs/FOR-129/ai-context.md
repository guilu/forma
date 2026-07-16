# FOR-129 AI Context

## Story

FOR-129 — Adherence read model: planned vs completed over N days per category. Second implementable slice (2 of 6) of FOR-104 [STUB] Progress & goals domain.

## Intent

Give the progress/consistency experiences (mockups 3/6, FOR-56 context) a real backend: how consistent the user has been vs plan over a rolling window, per category (training, nutrition, measurements). Pure derivation over existing data — no new persistence. Streaks, achievements, photos, muscle-map are later FOR-104 slices; goals (FOR-125) are done.

## Relevant Documents

- `specs/FOR-104/` — full progress/goals scope and slicing (this is slice 2).
- `AGENTS.md` — hexagonal boundaries, owner-scoping, no duplicated logic.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-005-api-design.md`.
- Mockups: `docs/3-entrenamiento.png`, `docs/6-progreso.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-129

## Domain Notes

- **Reuse, don't duplicate**:
  - TRAINING: `WeeklyTrainingScheduleService` (planned sessions), `TrainingSessionStatusService`/`TrainingSessionStatusRepository` (COMPLETED statuses), shared `WeeklyTrainingDayPolicy` (FOR-128). `TrainingAdherenceRules` already defines an adherence ratio concept for the weekly check-in — align terminology, but this story computes counts over an N-day window, not a single week.
  - NUTRITION: `MealLogRepository` (FOR-127) — days with at least one logged entry (MVP "planned" = days in window).
  - MEASUREMENTS: `BodyMeasurementRepository` — actual entries vs an expected cadence (documented, e.g. weekly).
- `SessionStatus` = PLANNED/COMPLETED/SKIPPED (completed = COMPLETED).
- No goals/streak/achievement data needed here.

## Architectural Constraints

- New `Adherence` read model + application service; NO domain aggregate to persist, NO migration (head stays V13).
- Hexagonal: derive from existing repositories/services; do not push counting logic into controllers.
- Owner-scoped per ADR-002.
- No duplicated counting/policy — the training window count must reuse the schedule + shared policy, not re-implement weekday logic.

## Common Pitfalls

- Divide-by-zero when a category has 0 planned — return null/0 rate, documented.
- Duplicating the weekday/training policy instead of reusing `WeeklyTrainingDayPolicy`/the schedule service.
- Inventing a per-day "planned meals" count for nutrition that doesn't exist — use days-with-log for MVP and document.
- Fabricating a measurement cadence — document the assumed one; don't hardcode silently.
- Returning 404 for empty data instead of zeroed categories.
- Building streaks/achievements here — those are separate slices.

## Suggested Implementation Order

1. `Adherence` read model type + a per-category planned/completed calculator (pure, tested with fixtures).
2. Application service wiring the three categories to their real repositories/services over the window.
3. `delivery/progress` controller + DTO `GET /api/v1/progress/adherence?days=` (+ API tests), with `days` validation.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: per-category planned/completed match hand-computed fixtures; TRAINING reuses schedule+status; NUTRITION uses days-with-log; MEASUREMENTS uses actual-vs-cadence; planned 0 → rate null/0 (no crash); `days` out of range → 400; empty data → zeroed categories (not 404); no new migration.
