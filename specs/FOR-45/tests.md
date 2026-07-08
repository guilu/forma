# FOR-45 Test Plan

## Scope

Verify the weekly insights endpoint: summary + recommendations + timestamp, main
selection, and empty-data handling.

## Domain Tests

N/A — models/rules are covered by FOR-40..FOR-44.

## Application Tests

- The insights service assembles the check-in, runs the rules, and selects the
  main recommendation by priority (`ACTION` > `WARNING` > `INFO`).
- Empty data → a valid result whose main recommendation is the insufficient-data
  one, with empty secondaries.

## API Tests

- `GET /api/v1/insights/weekly` returns the check-in summary, a main
  recommendation (message + reason + severity), any secondary recommendations,
  and a generated timestamp.
- With no data, the endpoint still returns `200` with an insufficient-data main
  recommendation (not an error).

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Only body or only training data → partial summary + applicable recommendations.
- Priority ties → stable documented ordering.

## Fixtures

- A populated week (body + training) producing multiple recommendations.
- An empty week (no data).
