# FOR-42 Test Plan

## Scope

Unit-test the body trend rules: each case produces the right recommendation with
a message and reason.

## Domain Tests

- **Positive trend** (weight stable, body fat down) → an `INFO` "keep the plan"
  recommendation with a reason citing the change.
- **Excessive weight drop** (weight dropping too quickly) → an `ACTION`
  review/slow-down recommendation.
- **Worsening trend** (body fat increasing) → an `ACTION` small-adjustment
  recommendation.
- **Insufficient data** (< 2 measurements) → an `INFO` "need more data"
  recommendation.
- Every emitted recommendation includes a non-blank message and reason.

## Application Tests

- The rules read the FOR-40 check-in / FOR-21 summary and produce the expected
  recommendation for a realistic week.

## API Tests

N/A — the API is FOR-45.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- At-threshold weight drop (inclusive/exclusive per the documented rule).
- Simultaneous signals (precedence / multiple recommendations per the documented
  rule).
- Missing body data.

## Fixtures

- Body summaries for each case: positive, excessive drop, worsening,
  insufficient data.
