# FOR-27: Mark training sessions as completed

Jira: https://dbhlab.atlassian.net/browse/FOR-27
Epic: FOR-3 Training Engine

## Summary

Let the user change a planned training session's status to `PLANNED`,
`COMPLETED` or `SKIPPED`, for both running and strength sessions, with optional
completion notes. The status is persisted and reflected in the weekly calendar
(FOR-26). No detailed workout logging in this MVP.

## User/System Flow

1. In the weekly calendar (FOR-26), the user marks a session completed or
   skipped.
2. The frontend calls a backend endpoint (through `apiClient`) to update the
   session status.
3. The backend persists the status (and optional note) and the calendar
   reflects the new state.

## Functional Requirements

- A session status model generic enough for **both** running and strength
  sessions: `PLANNED`, `COMPLETED`, `SKIPPED` (extends docs/domain-model.md's
  `StrengthWorkout.status` of PLANNED/COMPLETED with `SKIPPED` — document this
  addition).
- Status change is done in the domain/application layer (ADR-001), exposed via a
  backend endpoint (see `api.md`); controllers stay thin.
- Completion supports **optional notes** (stored or explicitly prepared).
- The frontend lets the user mark a running session and a strength session
  completed, and mark a session skipped, from the calendar.
- Completion status appears in the weekly calendar (FOR-26).
- No detailed workout logging (sets/reps actually done) in this story.

## Non-Functional Requirements

- Persisted status change; additive Flyway migration only if a new column/table
  is needed (ADR-003), never editing existing migrations.
- Input validated at the API boundary → the existing `VALIDATION_ERROR` shape
  (FOR-88 `GlobalExceptionHandler`); no ad-hoc error format.
- No secrets/PII in logs.

## Data Model Notes

Adds a status (and optional note) to a scheduled session instance. How running
(`RunningPlanSession`/scheduled instance) and strength (`StrengthWorkout`
instance) share a status representation should be decided so one endpoint can
update either (see Open Questions). Builds on FOR-22/FOR-25; does not add
workout-log detail.

## Edge Cases

- Marking an already-completed session again (idempotent) or reverting to
  planned — decide/document allowed transitions.
- Updating a non-existent session id — `NOT_FOUND` (404), per api-conventions.
- Optional note omitted vs. present — both round-trip correctly.

## Open Questions

- **Shared status model**: how running and strength sessions expose a common
  "mark status" contract (one polymorphic endpoint vs. per-type endpoints).
  Recommend a single status-update contract keyed by a session id + type;
  document the choice.
- **Scheduled instance identity**: FOR-22/FOR-25 model plans/templates; marking
  completion needs an identifiable scheduled session. If a scheduled-instance
  concept doesn't exist yet, define the minimal one here or document the gap —
  do not invent broad scope.
- Whether reverting `COMPLETED → PLANNED` is allowed in MVP.
