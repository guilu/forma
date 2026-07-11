# FOR-56 Test Plan

## Scope

Verify the insights surface renders the FOR-45 recommendation, its reason,
related signals, severity and disclaimer, with empty/error states.

## Domain Tests

N/A — insights rules covered by FOR-42/43/44.

## Application Tests

N/A.

## API Tests

N/A — consumes FOR-45 (mocked in UI tests).

## UI Tests

- Current insight card renders the main recommendation's message, severity badge
  and reason.
- Related signals (check-in evidence) render alongside the recommendation.
- Secondary recommendations render when present; only the main when not.
- A non-medical disclaimer is present.
- Empty data → the insufficient-data main recommendation renders (no error).

## Edge Cases

- No secondary recommendations → only the main shown.
- No history endpoint → history section hidden/deferred, not broken.
- Load error → error state with retry.

## Fixtures

- Mocked FOR-45 responses: ACTION main + secondaries; INFO insufficient-data
  main with empty secondaries; an error response.
