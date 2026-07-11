# FOR-60: Standardize loading, empty and error states

Jira: https://dbhlab.atlassian.net/browse/FOR-60
Epic: FOR-47 UI & UX

## Summary

Define reusable loading / empty / error state patterns and adopt them across every
MVP feature so missing data never reads as broken. Covers page + widget loading,
empty feature/filtered states, validation errors, recoverable API errors,
permission errors and a retry action. No raw exception text reaches users. Builds
on the existing `PagePlaceholder` and ADR-006 error conventions.

## User/System Flow

No direct user flow. Feature screens (FOR-51..FOR-59) consume the shared state
components instead of one-off messages.

## Functional Requirements

- Reusable components for: page loading, widget loading, empty feature state,
  empty filtered-result state, validation-error state, recoverable API-error
  state (with retry), permission/access-error state.
- Actionable, domain-aware messages; no stack/technical details (dev-only detail
  allowed behind a dev flag).
- Every MVP feature screen uses these patterns rather than custom messages.
- Loading states avoid layout jumps where reasonable (skeletons/placeholders).

## Non-Functional Requirements

- Consistent copy tone (calm, actionable); token-driven visuals (FOR-50).
- Accessible: states announced to assistive tech (feeds FOR-61).

## Data Model Notes

None — presentational. Aligns with ADR-006 frontend error handling and the
existing `components/PagePlaceholder.tsx`.

## Edge Cases

- Empty vs error must be visually distinct (no data ≠ failure).
- Retry that keeps failing → repeatable, non-blocking error.
- Permission error (future auth) → distinct from a generic error.

## Open Questions

- Skeleton vs spinner per surface — recommend skeletons for content areas, spinner
  for quick actions; document.
- Which existing pages to migrate in this story vs their own stories — recommend
  migrating all current pages (Dashboard/Body/Training/Nutrition/Shopping/
  Progress) and documenting any deferred.
