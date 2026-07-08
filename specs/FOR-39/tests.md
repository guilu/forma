# FOR-39 Test Plan

## Scope

Verify the Shopping page: list rendering with quantity/cost, check/uncheck, the
weekly total and monthly estimate, and empty/error states.

## Domain Tests

N/A — no domain logic in the frontend (ADR-006).

## Application Tests

N/A — no frontend state layer beyond the page component and API client.

## API Tests

N/A — this story tests the frontend's use of the API (mocked), not the API
itself.

## UI Tests

- Items are listed with name, quantity and estimated cost.
- The user can check and uncheck an item (calls the API; UI reflects the new
  state).
- The weekly total is visible with EUR formatting.
- The monthly estimate is visible with EUR formatting.
- An empty week shows the empty state (total 0), not a broken layout.

## Edge Cases

- Check-toggle API failure → error shown, list preserved.
- Long product names / large quantities do not break the mobile layout.
- API/network load failure → error state, not a crash.

## Fixtures

- A mocked weekly list with a few items (checked and unchecked) + budget.
- A mocked empty-week response.
- A mocked error response for the check toggle and for the initial load.
