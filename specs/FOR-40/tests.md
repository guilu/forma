# FOR-40 Test Plan

## Scope

Unit-test the `WeeklyCheckIn` model and its assembly from the FOR-21/FOR-28
summaries, including missing-data cases.

## Domain Tests

- A `WeeklyCheckIn` is created with body values and session counts set
  correctly.
- Absent body values are `null` (not fabricated); the check-in still builds.
- Session counts reflect the training summary.

## Application Tests

- The builder assembles a check-in from a `WeeklyBodySummary` (FOR-21) and a
  `WeeklyTrainingSummary` (FOR-28).
- A week with body data but no training (or vice versa) yields a partial
  check-in without error.
- No data at all yields a check-in with null body values and zero/absent
  training.

## API Tests

N/A — no HTTP endpoint in this story (FOR-45 exposes it).

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- No measurements → null body values.
- No planned/completed training → zero/absent counts.
- Partial data (body only, or training only).

## Fixtures

- A "full" week (body + training summaries populated).
- A "body only" week and a "training only" week.
- An "empty" week (both summaries empty).
