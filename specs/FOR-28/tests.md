# FOR-28 Test Plan

## Scope

Verify the weekly training summary calculation: planned/completed counts,
distances, and the empty state.

## Domain Tests

- Planned running/strength session counts are computed correctly for a week.
- Completed running/strength counts reflect only sessions marked completed.
- Total planned running distance sums all planned runs; completed running
  distance sums only completed runs.
- A completed run without a recorded distance is counted as completed but
  excluded from the completed-distance sum.

## Application Tests

- The summary use case reads the week's sessions + statuses via the repository
  and produces a complete result for a realistic mixed-completion week.
- Deterministic: same input yields the same summary.

## API Tests

N/A — no HTTP endpoint is required by this story.

## UI Tests

N/A — no frontend change in this story (dashboard display is separate).

## Edge Cases

- No planned sessions this week → empty-state result (not zero-filled without
  explanation).
- Sessions planned, none completed → completed counts 0.
- Missing completed-distance data → field absent/null, not fabricated.

## Fixtures

- A week with a mix of planned and completed running and strength sessions.
- A completed run with no distance recorded.
- An empty week (no planned sessions).
