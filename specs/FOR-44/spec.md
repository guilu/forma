# FOR-44: Create recovery warning recommendation rules

Jira: https://dbhlab.atlassian.net/browse/FOR-44
Epic: FOR-6 Insights Engine

## Summary

Detect simple, conservative recovery warning signals and emit `WARNING`
`Recommendation`s (FOR-41) suggesting review or a lighter week — never a
diagnosis. Rule-based over training completion (FOR-28) and body trend (FOR-21)
via the FOR-40 check-in.

## User/System Flow

This story has no direct user flow. It produces warnings consumed by FOR-45:

1. Given the week's completion pattern and body trend, the rules evaluate.
2. A matching rule emits a `RECOVERY` `Recommendation` at `WARNING` severity with
   a data-referencing reason.

## Functional Requirements

- Implement conservative recovery warning rules in the domain/application layer
  (ADR-001), reading the FOR-40 check-in (FOR-21 body + FOR-28 training).
- Cover cases such as:
  - **Several skipped sessions in a row** → suggest review / lighter week.
  - **Planned load increasing while completion is low** → suggest easing.
  - **Body trend worsening while training load is high** → suggest review.
- Use **`WARNING`** severity for recovery concerns; prefer suggesting review or a
  lighter week over drastic changes.
- Each recommendation includes a **reason**; do not diagnose health conditions.
- Handle missing data safely (no false warnings on absent data).

## Non-Functional Requirements

- Deterministic; conservative (avoid false alarms).
- Copy is neutral and non-alarming (docs/ui-guidelines.md).

## Data Model Notes

Consumes FOR-40 `WeeklyCheckIn` (FOR-21 body trend + FOR-28 completion). Produces
FOR-41 `Recommendation`s (`category = RECOVERY`, `severity = WARNING`). No new
persisted entity.

## Edge Cases

- Missing training or body data → no warning (fail safe, don't warn on absence).
- Borderline signals — keep conservative; document thresholds.
- Multiple signals at once — decide whether to emit one combined warning or
  several; document.

## Open Questions

- **Signal thresholds**: define conservative bounds (e.g. "several skipped" = N
  consecutive; "high load, low completion") and document them; Jira leaves exact
  numbers open. Note: "several skipped in a row" may need per-session history the
  current summaries don't expose — if so, document the data gap and implement the
  signals that available data supports.
- Whether a single combined `WARNING` or multiple — recommend one clear warning
  for the MVP; document.
