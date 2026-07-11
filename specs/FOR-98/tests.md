# FOR-98 Test Plan

## Scope

Verify the weekly training summary endpoint returns the FOR-28 counts + km via a
delivery DTO, for a populated and an empty week.

## Domain Tests

N/A — summary logic covered by FOR-28.

## Application Tests

N/A — reuses `WeeklyTrainingSummaryService`.

## API Tests

- `GET /api/v1/training/weekly-summary` returns planned/completed running &
  strength counts, total planned running km, completed running km, and the
  message (`@WebMvcTest`, service mocked).
- Empty week → zeros, 0.0 km, non-alarming message, 200 OK.
- The response DTO shape is distinct from the application record (no leakage).

## UI Tests

N/A — backend story.

## Edge Cases

- Zero planned sessions → safe response.
- Completed running km reflects only completed sessions (per FOR-28).

## Fixtures

- A mocked `WeeklyTrainingSummary` for a populated week and an empty week.
