# FOR-121 Test Plan

## Scope

Verify onboarding answers persist to the backend, the first-run gate reads
the backend flag, and the flow degrades gracefully when the backend is
unavailable.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-107 contract; no backend change in this story.

## UI Tests

- Completing a step persists that step's answers to the backend (mocked
  `PATCH`/onboarding call).
- Completing the full flow sets the backend's `firstRunCompleted` flag.
- Revisiting `/onboarding` after backend `firstRunCompleted: true` shows
  the "already completed" treatment, sourced from the backend flag, not
  only `localStorage`.
- Backend save failure mid-flow does not block navigation to the next
  step; a non-blocking message is shown.
- `localStorage` cleared mid-flow (simulated) with a backend that already
  has prior progress → recovers from the backend rather than restarting
  blind.

## Edge Cases

- Backend unreachable for the entire flow → user completes onboarding
  using only the local draft; a clear, non-blocking indication that
  server sync failed.
- Re-submitting onboarding after completion → accepted, not an error
  (mirrors FOR-107's documented edge case).

## Fixtures

- Mocked onboarding backend responses: successful save, failed save,
  already-completed state, and a fresh/first-run state.
