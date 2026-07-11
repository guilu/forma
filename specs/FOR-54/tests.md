# FOR-54 Test Plan

## Scope

Verify the nutrition screens render the day plan, macro summary and running-day
guidance from read models, with empty/error states.

## Domain Tests

N/A — nutrition domain covered by FOR-29..FOR-34.

## Application Tests

N/A.

## API Tests

N/A — consumes FOR-32/33/34 (mocked in UI tests).

## UI Tests

- Day-type selector switches between running/strength/rest plans.
- Daily meal list renders meals with per-meal macros and kcal.
- Macro summary shows calories vs target and the macro ring.
- Running day shows the late-run guidance (carbs earlier, lighter dinner).
- Recovery recommendation renders when present; hidden when absent.
- Shopping shortcut navigates to the shopping list.

## Edge Cases

- Rest day → no running-day flow.
- No plan for the day → empty state.
- Load error → error + retry.

## Fixtures

- Mocked day plans for running/strength/rest; a plan with and without a recovery
  recommendation; an error response.
