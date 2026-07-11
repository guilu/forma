# FOR-97 Test Plan

## Scope

Verify the weekly body summary endpoint returns the FOR-21 latest values +
deltas via a delivery DTO, honoring the null-delta honesty rules.

## Domain Tests

N/A — summary logic covered by FOR-21.

## Application Tests

N/A — reuses `WeeklyBodySummaryService`.

## API Tests

- `GET /api/v1/body/weekly-summary` returns latest weight/body-fat/lean-mass,
  weekly weight & body-fat deltas, `comparisonDays`, and the message
  (`@WebMvcTest`, service mocked).
- With fewer than two measurements → deltas and `comparisonDays` are null; 200 OK.
- With no measurements → latest values null, informative message.
- The response DTO shape is distinct from the domain record (no leakage).

## UI Tests

N/A — backend story.

## Edge Cases

- One measurement → latest present, deltas null.
- Gap > 7 days → `comparisonDays` reflects the real gap.

## Fixtures

- Mocked `WeeklyBodySummary` for: populated (with deltas), single measurement
  (null deltas), and empty (all null) cases.
