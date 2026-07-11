# FOR-60 Test Plan

## Scope

Verify the reusable state components render each state, offer retry where
applicable, and are adopted by feature screens without leaking technical details.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- Page-loading and widget-loading components render without layout jump.
- Empty feature state and empty filtered-result state render distinct messages.
- Validation-error, recoverable-API-error (with retry) and permission-error
  states render distinct, actionable messages.
- Retry action re-invokes the loader.
- No raw exception/stack text is rendered to the user.
- At least one migrated feature page uses the shared components.

## Edge Cases

- Empty vs error are visually/semantically distinct.
- Repeated retry failures stay non-blocking.

## Fixtures

- Mocked loading/empty/error scenarios; a retry handler; a dev-flag detail case.
