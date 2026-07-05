# FOR-19 Test Plan

## Scope

Verify the dashboard metric cards: correct latest-measurement values,
rounding, and the empty state when no measurements exist.

## Domain Tests

N/A — no domain logic in the frontend (ADR-006).

## Application Tests

N/A — no additional frontend state-management layer expected beyond the
page component and API client.

## API Tests

N/A — backend API tests are covered by `specs/FOR-17/tests.md`.

## UI Tests

- Renders weight, body fat %, fat mass, lean mass and BMI cards from the
  latest measurement (first item of the API response, given descending
  order).
- Values are rounded per the rules in spec.md (no extra fake precision).
- Zero measurements renders the empty state, not five empty/zero cards.
- A single measurement renders correctly (latest = only item).

## Edge Cases

- API returns an empty array.
- API returns exactly one item.
- API error/network failure — dashboard should not crash; show a reasonable
  error state consistent with ADR-006 ("every major screen must handle
  loading, empty and error states").

## Fixtures

- A mocked `GET /api/v1/body/measurements` response with two items (assert
  the first/most-recent one is used).
- A mocked empty-array response.
