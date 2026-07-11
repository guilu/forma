# FOR-53 Test Plan

## Scope

Verify the training screens render the weekly plan, session detail, exercise list
and completion, for running/strength/rest days, with empty/error states.

## Domain Tests

N/A — training domain covered by FOR-24..FOR-28.

## Application Tests

N/A.

## API Tests

N/A — consumes FOR-26/27/28 (mocked in UI tests).

## UI Tests

- Weekly calendar renders the current week with running/strength/rest days.
- A session detail opens for a running and for a strength session.
- The exercise list renders series/reps/rest per exercise.
- Marking a session completed calls the FOR-27 PATCH and reflects status in the
  calendar/summary.
- Weekly summary shows planned vs completed counts.
- Rest day renders with no session actions.

## Edge Cases

- Empty week → empty state.
- Completion failure → error, prior status preserved.
- Range/summary with zero planned sessions → safe copy.

## Fixtures

- Mocked week (running + strength + rest), a status-update success and failure,
  and an empty week.
