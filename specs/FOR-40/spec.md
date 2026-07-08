# FOR-40: Create weekly check-in model

Jira: https://dbhlab.atlassian.net/browse/FOR-40
Epic: FOR-6 Insights Engine

## Summary

Create the `WeeklyCheckIn` domain model: a snapshot of the user's week (latest
body values + planned/completed training) that the Insights rules (FOR-42/43/44)
and dashboard consume. Built from **existing** body (FOR-21) and training (FOR-28)
data; nutrition/shopping are out of scope for the first iteration. Missing data is
handled gracefully.

## User/System Flow

This story has no direct user flow. It defines the type consumed by later
stories:

1. An application service assembles a `WeeklyCheckIn` from the FOR-21
   `WeeklyBodySummary` and FOR-28 `WeeklyTrainingSummary`.
2. FOR-42/43/44 rules read it to produce recommendations.
3. FOR-45 exposes it (with recommendations) via the weekly insights endpoint.

## Functional Requirements

- Add `WeeklyCheckIn` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md "WeeklyCheckIn".
- Fields: `weekStartDate`, `latestWeightKg`, `latestBodyFatPercentage`,
  `latestLeanMassKg`, `plannedRunningSessions`, `completedRunningSessions`,
  `plannedStrengthSessions`, `completedStrengthSessions`, `notes`.
- The check-in can be **built from existing data** — an application builder
  assembles it from FOR-21 `WeeklyBodySummary` (latest body values) and FOR-28
  `WeeklyTrainingSummary` (session counts).
- **Missing data handled gracefully**: absent body/training values are
  `null`/absent, not fabricated (FOR-21/FOR-28 already return nulls for missing
  data).
- `notes` are optional/manual.
- Do **not** require nutrition or shopping data in this iteration.

## Non-Functional Requirements

- Framework-free, deterministic value type; no persistence introduced.
- No fake precision — carry the values from the summaries as-is.

## Data Model Notes

Mirrors docs/domain-model.md's `WeeklyCheckIn`, restricted to body + training
fields for this iteration (domain-model also lists `weeklyKm`,
`averageSleepHours` — include only if trivially available from FOR-28; otherwise
defer). This is the Insights-context check-in, distinct from the Body-context
`WeeklyBodySummary` (FOR-21) and Training-context `WeeklyTrainingSummary` (FOR-28)
that feed it.

## Edge Cases

- No measurements yet — body values are null; the check-in still builds.
- No planned/completed training — session counts are zero/absent.
- A week with body data but no training data (or vice versa) — partial check-in.

## Open Questions

- Whether `WeeklyCheckIn` stores raw values only, or also the body **change**
  (weekly weight/body-fat delta from FOR-21) needed by FOR-42 rules. Recommend
  carrying the deltas (or letting rules read `WeeklyBodySummary` directly) —
  document so FOR-42 has what it needs.
- Whether to include `weeklyKm`/`averageSleepHours` now — recommend only if
  FOR-28 already provides them; otherwise defer.
