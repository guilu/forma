# FOR-26 Test Plan

## Scope

Verify the weekly training calendar: rendering of running/strength/rest entries
from plan data, and the empty/error states.

## Domain Tests

N/A — no domain logic in the frontend (ADR-006).

## Application Tests

N/A — no frontend state layer beyond the page component and API client.

## API Tests

N/A — this story tests the frontend's use of the plan data (mocked), not a
backend API.

## UI Tests

- The week renders 7 days; planned running sessions appear on their days.
- Planned strength sessions appear on their days.
- Rest days (no session) are clearly shown.
- Each entry shows session type, distance-or-workout-name, and status.
- A week with no sessions shows the empty state, not a broken grid.

## Edge Cases

- A day with both a run and a strength session (both rendered).
- API/network failure shows an error state (ADR-006), no crash.
- Long workout names / distances do not break the mobile layout.

## Fixtures

- A mocked weekly-plan response with a mix of running, strength and rest days.
- A mocked empty-week response.
- A mocked error/failed response.
