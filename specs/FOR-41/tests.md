# FOR-41 Test Plan

## Scope

Unit-test the `Recommendation` model: the constrained category/severity, the
required message + reason, and the optional related metric.

## Domain Tests

- A valid recommendation is created with category, severity, message and reason.
- `category` only accepts `BODY`, `TRAINING`, `NUTRITION`, `RECOVERY`,
  `SHOPPING` (enum).
- `severity` only accepts `INFO`, `WARNING`, `ACTION` (enum).
- Construction rejects a blank `message` or blank `reason`.
- The optional related metric is absent when not provided.

## Application Tests

N/A — no application/use-case layer is introduced by this story.

## API Tests

N/A — the API is FOR-45.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Blank message / blank reason.
- No related metric (optional).

## Fixtures

- An `INFO` body recommendation and a `WARNING` recovery recommendation, each
  with message + reason.
