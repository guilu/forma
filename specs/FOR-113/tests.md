# FOR-113 Test Plan

## Scope

Verify the onboarding page-level heading, the `ProductEditModal` shared-
component migration, and the wired `ErrorState` dev-only detail.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- `OnboardingPage` renders exactly one `<h1>`.
- `ProductEditModal` in its loading state renders the shared `LoadingState`
  component (not the old inline paragraph).
- `ProductEditModal` in its error state renders the shared `ErrorState`
  component with the existing `PRODUCT_LOAD_ERROR` message.
- `ProductEditModal` in its not-found state renders the chosen shared
  component (`EmptyState` or `ErrorState`) with the existing
  `PRODUCT_NOT_FOUND` message, and remains distinguishable from the error
  state.
- The chosen `ErrorState` caller renders `detail` when `showDetail` is
  true, and never renders it when `showDetail` is false (regression guard
  for the "never in production" contract).

## Edge Cases

- Onboarding heading persists identically across step navigation (no
  duplicate or missing `<h1>` mid-flow).
- `ProductEditModal`'s not-found and error states remain visually and
  semantically distinct after migration (no message collapsing).

## Fixtures

- Mocked `listShoppingProducts` responses producing each of
  `ProductEditModal`'s three non-ready states (loading pending, rejected,
  resolved-without-a-match) for the migration tests.
