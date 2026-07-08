# FOR-44 AI Context

## Story

FOR-44 — Create recovery warning recommendation rules
(https://dbhlab.atlassian.net/browse/FOR-44)

## Intent

Help the user avoid pushing intensity when data suggests fatigue. Success is
conservative `WARNING` recovery recommendations (review / lighter week) with
data-referencing reasons — never diagnostic, never alarming.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (neutral, non-alarming copy)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-40/`, `specs/FOR-41/`, `specs/FOR-21/`, `specs/FOR-28/`
- Jira: https://dbhlab.atlassian.net/browse/FOR-44

## Domain Notes

- Combine FOR-28 completion (low completion, rising load) and FOR-21 body trend
  (worsening) via the FOR-40 check-in.
- Emit FOR-41 `Recommendation`s (`category = RECOVERY`, `severity = WARNING`).
  Suggest review / lighter week; never diagnose.
- Some signals (e.g. "several skipped in a row") may need per-session history the
  current summaries don't expose — document the gap and implement what available
  data supports.

## Architectural Constraints

- Pure rule logic in the domain (a `RecoveryWarningRules` evaluator), optionally
  wrapped by an application service.
- No persistence, no controller logic.

## Common Pitfalls

- False warnings on missing data (must fail safe).
- Diagnostic or alarming language.
- Drastic-change suggestions instead of "review / lighter week".

## Suggested Implementation Order

1. Define conservative signal thresholds (document them) and the evaluator.
2. Implement the supported signals → a `WARNING` recommendation.
3. Fail safe on missing data (no warning).
4. Unit-test each warning case and the missing-data case; verify neutral copy.

## Validation

Run `./gradlew test` from `backend/`.
