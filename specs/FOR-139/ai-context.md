# FOR-139 AI Context

## Story

FOR-139 — Streak + weekly-history bars API. Streak-&-weekly-history slice (slice 3) of FOR-104
[STUB] Progress & goals domain. Unblocks FOR-53 streak ("RACHA ACTUAL") + weekly-history bars.

## Intent

Provide the read models behind the FOR-53 streak and weekly-history bars, which shipped as
documented placeholders. Success = `GET /progress/streak` and `GET /progress/weekly-history`
return auditable data feeding mockup 3. Pure derivation; no persistence.

## Key finding (verified) — net-new, with a hard data constraint

- **Not implemented today** — no streak/weekly-history types or endpoints exist.
- **No per-date training completion history exists.** `training_session_status` (V3) stores only the *current* status per weekday slot (documented in `AdherenceService`). The streak MUST therefore be built on real per-date facts, NOT training completion:
  - `MealLogRepository.findByOwnerAndDate(ownerId, date)` — nutrition logging dates (owner-scoped, strongest per-date fact).
  - `BodyMeasurementRepository.list()` + `measuredAt` — measurement dates.
  - `WaterIntakeEntry` (V14) — dated, optional.

## Relevant Documents

- `specs/FOR-104/` — full stub scope; slice-3 details.
- `specs/FOR-135/`, `specs/FOR-136/` — sibling delivered slices; `ProgressController` is the FOR-135 surface to extend.
- `AGENTS.md` — hexagonal, explainable/auditable, no duplicated logic, repo-state-over-spec.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md` (owner-scoping), `ADR-003-persistence.md` (no migration), `ADR-005-api-design.md`.
- Mockup: `docs/3-entrenamiento.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-139

## Domain / Repo Notes (verified)

- Extend `delivery/progress/ProgressController` (already hosts `/adherence`, `/achievements`) with `/streak` and `/weekly-history`.
- New application read models: `Streak` (currentStreakDays/longestStreakDays/asOf) and `WeeklyHistory` (ordered buckets). No persisted aggregate.
- Reuse `WeeklyTrainingScheduleService`/`WeeklyTrainingSummary`/`SessionStatus` only where a signal is genuinely available; reuse `MealLogRepository`/`BodyMeasurementRepository` for per-date facts.

## Architectural Constraints

- Read models only — no migration (ADR-003; head stays V18).
- Streak rule is documented explicitly (spec.md + code javadoc), auditable from source dates.
- Bounded-window queries; acceptable per-request cost at MVP volume.
- No duplicated schedule/policy logic; avoid re-deriving the weekday policy.

## Common Pitfalls

- Basing the streak on per-date training completion — that history does not exist; use nutrition/measurement dates.
- Leaving the streak rule implicit — it MUST be documented (gap reset, today-inclusivity, qualifying activity).
- Omitting empty weeks from weekly-history — return zero buckets so bars still render.
- Adding a migration for what is pure derivation.
- Fabricating per-date training completion for the history bars.

## Suggested Implementation Order

1. `Streak` read model + rule (consecutive consistent days from per-date facts; gap reset; longest run) — pure, tested with boundary + gap fixtures.
2. `WeeklyHistory` read model — bounded per-week buckets from the chosen honest signal.
3. Application services (owner-scoped where possible), reusing existing repositories/services.
4. Delivery: `GET /progress/streak` + `GET /progress/weekly-history` + DTOs (+ API tests), empty → zeroed, bounded-param → 400.

## Validation

Backend build + tests (`./gradlew build`). Confirm: streak follows the documented rule (continuation,
gap reset, empty → 0); weekly-history returns bounded per-week buckets incl. zero weeks; no migration
(head V18); no duplicated schedule logic; no regression to training summary/status or adherence.
