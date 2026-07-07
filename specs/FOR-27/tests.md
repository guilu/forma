# FOR-27 Test Plan

## Scope

Verify the session status change end to end: domain/application transition rules,
the status-update endpoint, and the frontend mark interaction.

## Domain Tests

- A session transitions `PLANNED → COMPLETED` and `PLANNED → SKIPPED`.
- Disallowed transitions (per the documented rule) are rejected.
- Optional completion note is attached when provided, absent otherwise.

## Application Tests

- The status-change use case updates the session via the repository and returns
  the new state.
- Updating a non-existent session id surfaces a not-found outcome.

## API Tests

- The status-update endpoint changes a running session's status and returns it.
- The same endpoint changes a strength session's status.
- Invalid status value → `VALIDATION_ERROR` (400) with field details.
- Unknown session id → `NOT_FOUND` (404).

## UI Tests

- From the calendar, the user can mark a running session completed and a
  strength session completed.
- The user can mark a session skipped.
- The calendar reflects the updated status after the action.
- API failure shows an error state, not a crash (ADR-006).

## Edge Cases

- Marking an already-completed session (idempotency/allowed transition).
- Note omitted vs. present (round-trip).
- Reverting completed → planned (only if allowed by the documented rule).

## Fixtures

- A scheduled running session and a scheduled strength session in `PLANNED`.
- Mocked API success and error (`VALIDATION_ERROR`, `NOT_FOUND`) responses for
  the frontend tests.
