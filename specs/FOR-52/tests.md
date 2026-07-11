# FOR-52 Test Plan

## Scope

Verify the body composition screen renders latest metrics, trend, history and
manual entry, distinguishes sources, and handles empty/error states.

## Domain Tests

N/A — body domain covered by FOR-15/FOR-21.

## Application Tests

N/A.

## API Tests

N/A — consumes FOR-17 (mocked in UI tests).

## UI Tests

- Latest metric cards render values + "vs semana pasada" deltas.
- Weight evolution chart renders with a range selector.
- History table lists recent measurements with the right columns.
- Manual entry form submits and shows field-level validation errors close to
  fields; the list refreshes/preserves on error.
- Imported vs manual measurements are visually distinguishable.
- Empty state (no measurements) shows an entry CTA, not a broken layout.

## Edge Cases

- One measurement → cards without deltas.
- Load error → error state with retry; entry still reachable.
- Unknown source → neutral source label.

## Fixtures

- Mocked measurement lists: empty, single, multi (mixed manual/imported);
  a create success and a validation-error response.
