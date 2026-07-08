# FOR-43 AI Context

## Story

FOR-43 — Create training adherence recommendation rules
(https://dbhlab.atlassian.net/browse/FOR-43)

## Intent

Let training consistency inform weekly guidance without shaming. Success is a
small set of rule-based `TRAINING` recommendations (high/low adherence,
running/strength imbalance) with data-referencing reasons.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (no guilt/streak language; neutral, actionable)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-40/`, `specs/FOR-41/`, `specs/FOR-28/` (training summary)
- Jira: https://dbhlab.atlassian.net/browse/FOR-43

## Domain Notes

- Read the FOR-28 `WeeklyTrainingSummary` (planned/completed running & strength)
  via the FOR-40 check-in.
- Produce FOR-41 `Recommendation`s (`category = TRAINING`). Reason cites the
  completion counts. Never shame missed sessions.

## Architectural Constraints

- Pure rule logic in the domain (a `TrainingAdherenceRules` evaluator),
  optionally wrapped by an application service (FOR-21/FOR-28 precedent).
- No persistence, no controller logic.

## Common Pitfalls

- Guilt/streak language for missed sessions.
- Dividing by zero when no sessions are planned.
- A recommendation without a reason.

## Suggested Implementation Order

1. Define high/low adherence thresholds (document them) and the evaluator.
2. Implement the four cases → `Recommendation`s, neutral copy.
3. Handle no-planned-training safely.
4. Unit-test each rule (message + reason + severity) and the missing-data case.

## Validation

Run `./gradlew test` from `backend/`.
