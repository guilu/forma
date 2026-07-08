# FOR-43 Test Plan

## Scope

Unit-test the training adherence rules: each case produces the right
recommendation with message and reason, and missing data is handled.

## Domain Tests

- **High adherence** → a positive `INFO` recommendation citing the completion.
- **Low adherence** → a non-shaming `INFO`/`ACTION` recommendation to rebuild
  consistency.
- **Running done, strength missed** → a balance recommendation.
- **Strength done, running missed** → a balance recommendation.
- Every emitted recommendation includes a non-blank message and reason.

## Application Tests

- The rules read the FOR-28 summary via the FOR-40 check-in and produce the
  expected recommendation for a realistic week.

## API Tests

N/A — the API is FOR-45.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- No planned sessions → safe result (no divide-by-zero, no shame).
- At-threshold adherence (inclusive/exclusive per the documented rule).
- Fully completed vs. fully missed weeks.

## Fixtures

- Training summaries for each case: high, low, running-only, strength-only, and
  no-planned-training.
