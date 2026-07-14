# FOR-122 Test Plan

## Scope

Verify the Objetivos screen lists, details and (if in scope) edits goals
from the FOR-104 domain, with correct loading/empty/error handling.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — depends entirely on FOR-104's (not-yet-specified) API; no backend
change owned by this story.

## UI Tests

- `GoalsPage` no longer renders `PagePlaceholder`; renders the real goal
  list once FOR-104 data is available.
- No-goals state renders `EmptyState`, not a blank screen.
- Load failure renders `ErrorState` with a working retry.
- Selecting a goal opens its detail view with the correct data.
- Editing a goal (if in scope) persists the change and reflects it in the
  list/detail.

## Edge Cases

- FOR-104 unavailable at implementation time → this story cannot be
  meaningfully tested against a real contract; tests should be written
  against FOR-104's actual shape once it exists, not a guessed one.

## Fixtures

- Mocked goal-list/detail responses once FOR-104's shape is known; do not
  fabricate a shape ahead of FOR-104's own spec.
