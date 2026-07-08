# FOR-44 Test Plan

## Scope

Unit-test the recovery warning rules: supported signals emit a conservative
`WARNING` with a reason, and missing data never triggers a false warning.

## Domain Tests

- **Rising planned load with low completion** → a `RECOVERY` `WARNING`
  recommending review / lighter week, with a reason.
- **Body trend worsening while training load is high** → a `RECOVERY` `WARNING`.
- A "several skipped in a row" signal, if data supports it, → a `WARNING`;
  otherwise the data gap is documented and untested.
- Every emitted warning includes a non-blank reason and non-alarming copy.

## Application Tests

- The rules read the FOR-40 check-in and emit the expected warning for a
  realistic fatigued week.

## API Tests

N/A — the API is FOR-45.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Missing training/body data → **no** warning (fail safe).
- Borderline signals just below threshold → no warning.
- Multiple signals → one combined warning (per the documented decision).

## Fixtures

- A "fatigued" week (high load, low completion, worsening body trend).
- A "healthy" week (no warning).
- An "empty" week (no data → no warning).
