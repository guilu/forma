# FOR-59 Test Plan

## Scope

Verify the onboarding flow navigates steps, validates input, supports skip/resume
and ends with a clear next action.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — no onboarding backend (local/deferred persistence).

## UI Tests

- The flow renders steps in order with progress indication.
- Next/back navigate; skip advances past non-critical steps.
- A step with invalid input blocks advance and shows a clear error.
- Completing the flow routes to the dashboard with a next action.
- Progress is restored on resume where local persistence exists.

## Edge Cases

- Skipping all non-critical steps still completes onboarding.
- Validation error → understandable message, no advance.
- Mobile → steps usable, no horizontal scroll.

## Fixtures

- Step definitions; a valid path and an invalid-input path; a resume scenario.
