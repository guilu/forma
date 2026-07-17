# FOR-139 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-139
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (streak-&-weekly-history slice, slice 3). Unblocks FOR-53 streak/history.

## Summary

Two read models, computed on demand from existing history, exposed under `/api/v1/progress`:

- **Streak**: a consecutive-period consistency streak — `currentStreakDays`, `longestStreakDays`, `asOf`.
- **Weekly-history**: a per-week series (bars) over a bounded window.

Pure derivation. **No new persistence, no migration** (head stays V18). Unblocks the FOR-53
"RACHA ACTUAL" and weekly-history bars, which shipped as documented placeholders (no endpoint).

## Repository baseline (verified)

- **This slice is NOT implemented** — no `streak`/`weekly-history` types or endpoints exist (searched).
- `delivery/progress/ProgressController` already hosts `/progress/adherence` (FOR-129) and `/progress/achievements` (FOR-135) — mount the new read endpoints here (thin controller).
- **Per-date history constraint (KEY, verified):** `training_session_status` (V3) stores only the *current* status per weekday slot — **there is no per-date training completion history** (already documented in `AdherenceService`). Real per-date, owner-scoped facts that DO exist:
  - `MealLogRepository.findByOwnerAndDate(ownerId, date)` — nutrition logging dates (owner-scoped).
  - `BodyMeasurementRepository.list()` with `measuredAt` — measurement dates.
  - `WaterIntakeEntry` (V14) — dated water intake, if useful.
- Reuse `WeeklyTrainingSummary`/`WeeklyTrainingScheduleService` and `SessionStatus` where a signal is genuinely available; do not duplicate the schedule/policy logic.

## Streak rule — MUST be explicit and documented (auditable)

The streak MUST be defined on a real per-date signal, NOT on training completion (no per-date
history exists). **Proposed rule (finalize in design):**

- A **consistent day** = a calendar day (owner timezone) on which the owner recorded at least one **qualifying activity**. Qualifying activity for MVP = a logged nutrition meal-log entry for that date (`MealLogRepository.findByOwnerAndDate`), because it is the strongest real per-date owner-scoped fact. Optionally also count a `BodyMeasurement` whose `measuredAt` falls on that day — decide + document in design.
- **`currentStreakDays`** = the number of consecutive consistent days ending at `asOf` (today). Document the today-inclusivity rule (does an inactive-so-far today break the streak, or is today grace until end of day?) — pick one and document it.
- **A gap day** (no qualifying activity) resets the current streak to 0.
- **`longestStreakDays`** = the longest run of consecutive consistent days within the bounded lookback window.
- **`asOf`** = the date the streak was computed (owner timezone).

Empty history → `currentStreakDays: 0`, `longestStreakDays: 0`, not an error.

## Weekly-history rule — documented buckets

- A bounded series of the last **N weeks** (default e.g. 8–12; document the choice) ending with the current week.
- Each bucket = per-week `planned` vs `completed` (or a volume measure) for the bars.
- **Signal honesty:** buckets sourced from real per-date facts (nutrition/measurements) are exact; a training bucket inherits the `AdherenceService` limitation (current-week pattern projected — no per-date history). Document which signal each bar uses; do NOT fabricate per-date training completion.
- Weeks with no data → zero-valued bucket, still present in the series (so bars render), never omitted-as-error.

## User/System Flow

1. FOR-53 (Progreso/Entrenamiento, mockup 3) GETs `/api/v1/progress/streak` and `/api/v1/progress/weekly-history`.
2. Backend derives each read model from bounded-window queries over existing history and returns the shapes in `api.md`.

## Functional Requirements

- `GET /api/v1/progress/streak` → current + longest streak per the documented rule.
- `GET /api/v1/progress/weekly-history` → per-week series over a bounded window.
- Read models only; owner-scoped per ADR-002 where the source supports it (same documented gap as FOR-129 for non-owner-scoped tables).
- Bounded windows; acceptable per-request computation at MVP volume.

## Non-Functional Requirements

- **No migration** — pure derivation (ADR-003; head stays V18).
- Explainable/auditable from source dates; the streak rule is documented in both `spec.md` and code javadoc.
- No duplicated schedule/policy logic — reuse existing services.

## Data Model Notes

- New read models (application layer): a `Streak` (current/longest/asOf) and a `WeeklyHistory` (ordered buckets). No persisted aggregate, no table.
- Derive from `MealLogRepository`/`BodyMeasurementRepository` (+ optionally `WaterIntakeEntry`) for real per-date facts; reuse `WeeklyTrainingScheduleService`/`WeeklyTrainingSummary` only where honest.

## Edge Cases

- Empty history → streak 0/0; weekly-history all-zero buckets (series still present).
- Single active day today → `currentStreakDays` = 1 (per the documented today-inclusivity rule).
- Gap day inside a run → current streak resets; longest preserved.
- Window boundary (oldest week partially outside window) → document inclusivity.

## Open Questions

- Exact qualifying-activity set for a "consistent day" (nutrition only, or nutrition + measurement + water?) — decide in design.
- Today-inclusivity for `currentStreakDays` (grace until end of day vs strict).
- Weekly-history window length (N weeks) and per-bar signal (planned/completed vs volume), confirmed against docs/3-entrenamiento.png.
- Whether the training bar is worth showing given the no-per-date-history limitation, or whether history bars should be nutrition/measurement-based for MVP.
