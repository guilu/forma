# FOR-22: Create running plan domain model

Jira: https://dbhlab.atlassian.net/browse/FOR-22
Epic: FOR-3 Training Engine

## Summary

Define the `RunningPlanSession` domain model: a framework-free type describing
one **planned** running session in a multi-week progression (week number, day,
session type, target distance, target effort, notes). This story is domain-only
— no persistence, API or UI. Actual run logs (`RunningSession`) are a later
concern.

## User/System Flow

This story has no user-facing flow. It defines the type consumed by later
stories:

1. FOR-23 seeds a 16-week plan as a collection of `RunningPlanSession`.
2. FOR-26 renders planned sessions in the weekly calendar.
3. FOR-27 lets a planned session be marked completed/skipped.

## Functional Requirements

- Add `RunningPlanSession` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (no Spring/JPA/JDBC/HTTP — ADR-001), following the FOR-15 `BodyMeasurement`
  precedent.
- Fields (docs/domain-model.md "RunningPlanSession"): `weekNumber`,
  `dayOfWeek`, `sessionType`, `targetDistanceKm`, `targetEffort` (RPE-style),
  `notes`. `targetPaceRange` is optional per domain-model; include only if
  cheap, otherwise document as deferred.
- `sessionType` constrained to a known set: `EASY`, `LONG_RUN`, `INTERVALS`,
  `RECOVERY` (matches docs/domain-model.md).
- `dayOfWeek` uses a standard type (e.g. `java.time.DayOfWeek`) rather than a
  free-form string.
- The model must support a multi-week plan (a `weekNumber` per session; no
  single-week assumption).
- Keep the model independent from any external running platform (no provider
  ids/tokens — docs/architecture-overview.md Integration model).

## Non-Functional Requirements

- Performance: pure in-memory value; no external calls.
- Security: no provider credentials pass through this type.
- Observability: none at this layer; do not log training data here.

## Data Model Notes

Mirrors docs/domain-model.md's `RunningPlanSession` (planned session), distinct
from `RunningSession` (an actual completed run) which is **not** created by this
story. Persistence for the plan is FOR-23's concern; this story introduces no
table.

## Edge Cases

- `targetDistanceKm` zero or negative — decide whether the type rejects at
  construction (recommended, per FOR-15 precedent) or defers to a later layer.
- `sessionType` outside the known set — impossible if modelled as an `enum`.
- `weekNumber`/`dayOfWeek` out of expected range (e.g. week 0) — validate or
  document.

## Open Questions

- Include `targetPaceRange` now or defer until a story needs pace display?
  Recommend deferring (Jira emphasises "target effort rather than only pace",
  see FOR-23) and keeping `targetEffort` as the primary intensity field.
- Validate values at construction (FOR-15 precedent) vs. plain data holder —
  recommend construction-time validation for internal consistency.
