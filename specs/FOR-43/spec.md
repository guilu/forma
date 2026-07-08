# FOR-43: Create training adherence recommendation rules

Jira: https://dbhlab.atlassian.net/browse/FOR-43
Epic: FOR-6 Insights Engine

## Summary

Generate `Recommendation`s (FOR-41) from weekly training completion, so
consistency informs weekly guidance and explains whether progress data is
meaningful. Rule-based over the FOR-28 `WeeklyTrainingSummary` (via the FOR-40
check-in); neutral, non-shaming language.

## User/System Flow

This story has no direct user flow. It produces recommendations consumed by
FOR-45:

1. Given the week's planned vs. completed running/strength counts, the rules
   evaluate.
2. Each matching rule emits a `Recommendation` (category `TRAINING`) with a
   message and a data-referencing reason.

## Functional Requirements

- Implement rule-based training recommendations in the domain/application layer
  (ADR-001), reading the FOR-28 `WeeklyTrainingSummary` via the FOR-40 check-in.
- Cover the cases:
  - **High adherence**: most planned training completed → positive `INFO`.
  - **Low adherence**: little planned training completed → `INFO`/`ACTION`
    encouraging consistency (non-shaming).
  - **Running done, strength missed** → `INFO`/`ACTION` to balance.
  - **Strength done, running missed** → `INFO`/`ACTION` to balance.
- Each recommendation includes a **message and a reason** referencing the
  completion counts.
- Handle **missing training data** (no planned sessions) safely.
- Keep language neutral and actionable; do not shame missed sessions.

## Non-Functional Requirements

- Deterministic: same completion data always yields the same recommendation(s).
- Neutral copy (docs/ui-guidelines.md: no guilt language).

## Data Model Notes

Consumes FOR-28 `WeeklyTrainingSummary` (planned/completed running & strength).
Produces FOR-41 `Recommendation`s. No new persisted entity.

## Edge Cases

- No planned sessions this week → a safe "no training planned" result (no shame,
  possibly no recommendation or an `INFO`).
- Exactly at the high/low adherence threshold — decide inclusive/exclusive;
  document.
- Both running and strength fully completed vs. fully missed.

## Open Questions

- **Adherence thresholds**: define "high" and "low" completion bounds (e.g. ≥ 80%
  high, ≤ 40% low) and document; Jira leaves exact numbers open.
- Whether imbalance rules and adherence rules can both fire — recommend a small,
  prioritized set; document precedence.
