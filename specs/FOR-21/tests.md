# FOR-21 Test Plan

## Scope

Verify the weekly body summary calculation: latest values, weekly change
calculation, insufficient-data handling, and the status message.

## Domain Tests

- Latest weight/body fat %/lean mass extracted correctly from a set of
  measurements.
- Weekly weight change and weekly body fat change computed correctly when a
  valid prior-week measurement exists.
- Weekly change fields are absent/null (not `0`) when no suitable prior
  measurement exists.
- Status message reflects the actual computed state (e.g. mentions
  insufficient data when applicable) without prescriptive/gamified language.

## Application Tests

- Summary use case reads measurements via the FOR-16 repository and produces
  a complete summary result for a realistic multi-week measurement history.

## API Tests

N/A — no new HTTP endpoint is required by this story (see spec.md).

## UI Tests

N/A — no frontend change in this story.

## Edge Cases

- Zero measurements.
- Exactly one measurement (latest values only, no weekly change).
- Two measurements more than a week apart (change either omitted or
  clearly qualified, per the documented comparison rule).
- Multiple measurements within the same week (decide/document which one
  counts as "latest").

## Fixtures

- A measurement history with a clean one-week gap (normal weekly-change
  case).
- A single-measurement history (insufficient-data case).
- An empty history (no-data case).
- A history with an irregular gap (>1 week between the two most recent
  measurements).
