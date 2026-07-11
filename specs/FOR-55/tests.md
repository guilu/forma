# FOR-55 Test Plan

## Scope

Verify the shopping screen renders the category-grouped list + budget, toggles
checked state, surfaces edit entry points, and handles empty/error states.

## Domain Tests

N/A — shopping domain covered by FOR-35..FOR-39.

## Application Tests

N/A.

## API Tests

N/A — consumes FOR-38/39/36 (mocked in UI tests).

## UI Tests

- List renders grouped by category with filter tabs.
- Each item shows name, quantity + unit, estimated price and checked state.
- Toggling an item persists via the API and updates the row; on failure the list
  is preserved and an error shown.
- Budget summary shows product count, weekly total and monthly estimate with EUR
  formatting.
- A product price/URL edit entry point is reachable.
- Empty list → clear empty state, total `0,00 €`.

## Edge Cases

- Unknown category → grouped under "Otros".
- Check toggle failure → revert/error, list kept.
- Empty week → no broken layout.

## Fixtures

- Mocked list (multiple categories) + budget; a toggle success and failure; an
  empty list.
