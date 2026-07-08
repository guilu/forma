# FOR-42: Create body trend recommendation rules

Jira: https://dbhlab.atlassian.net/browse/FOR-42
Epic: FOR-6 Insights Engine

## Summary

Generate conservative, explainable `Recommendation`s (FOR-41) from body
composition trends, so the user knows whether to keep, adjust or review the plan.
Rule-based over the FOR-40 check-in / FOR-21 body summary; no medical claims;
always includes the reason.

## User/System Flow

This story has no direct user flow. It produces recommendations consumed by
FOR-45:

1. Given the week's body trend (weight/body-fat change from FOR-21), the rules
   evaluate.
2. Each matching rule emits a `Recommendation` (category `BODY`) with a message
   and a data-referencing reason.

## Functional Requirements

- Implement rule-based body recommendations, in the domain/application layer
  (ADR-001), reading the FOR-40 check-in / FOR-21 `WeeklyBodySummary`.
- Cover the cases:
  - **Positive trend**: weight stable and body fat improving → keep the plan
    (`INFO`).
  - **Excessive weight drop**: weight dropping too quickly → review/slow down
    (`ACTION`, conservative).
  - **Worsening trend**: body fat increasing across measurements → small
    adjustment (`ACTION`).
  - **Insufficient data**: not enough measurements to decide → `INFO` "need more
    data".
- Each recommendation includes a **message and a reason** referencing the
  observed body data.
- Keep rules conservative; prefer small weekly adjustments; no medical claims.

## Non-Functional Requirements

- Deterministic: same body trend always yields the same recommendation(s).
- Neutral, non-alarming copy (docs/ui-guidelines.md).

## Data Model Notes

Consumes FOR-40 `WeeklyCheckIn` (or FOR-21 `WeeklyBodySummary` directly) — latest
values + weekly weight/body-fat deltas. Produces FOR-41 `Recommendation`s. No new
persisted entity.

## Edge Cases

- Fewer than two measurements → insufficient-data recommendation (no trend).
- Exactly-at-threshold changes (e.g. weight drop right at the "too quickly"
  bound) — decide inclusive/exclusive and document.
- Simultaneous signals (e.g. weight stable + body fat up) — decide precedence /
  whether multiple recommendations are emitted; document.

## Open Questions

- **Thresholds**: define conservative numeric bounds (e.g. weekly weight drop >
  ~1% as "too quickly"; body fat rising over N measurements) and document them in
  code/tests — Jira leaves exact numbers open.
- Whether rules emit at most one body recommendation or several — recommend a
  small, prioritized set; document precedence.
