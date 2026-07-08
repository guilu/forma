# FOR-42 AI Context

## Story

FOR-42 — Create body trend recommendation rules
(https://dbhlab.atlassian.net/browse/FOR-42)

## Intent

Turn body trends into conservative, explainable guidance (keep / adjust /
review). Success is a small set of rule-based `BODY` recommendations, each with a
data-referencing reason, covering positive/excessive-drop/worsening/insufficient
cases.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (calm, factual, no gamification; example insight copy)
- `docs/domain-model.md` (Insights → Recommendation example rules)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-40/` (check-in), `specs/FOR-41/` (recommendation), `specs/FOR-21/`
  (body summary + weekly deltas)
- Jira: https://dbhlab.atlassian.net/browse/FOR-42

## Domain Notes

- Read the FOR-21 `WeeklyBodySummary` deltas (`weeklyWeightChangeKg`,
  `weeklyBodyFatChange`) via the FOR-40 check-in.
- Produce FOR-41 `Recommendation`s (`category = BODY`). Reason must cite the
  observed change; no medical claims; prefer small weekly adjustments.
- docs/ui-guidelines.md gives the tone (e.g. "Body fat is down 0.3% while weight
  is stable — keep calories unchanged").

## Architectural Constraints

- Pure rule logic in the domain (a `BodyTrendRules` / evaluator), optionally
  wrapped by an application service — like the FOR-21/FOR-28 calc precedents.
- No persistence, no controller logic.

## Common Pitfalls

- Medical claims or alarming copy.
- Aggressive adjustments instead of small weekly ones.
- Emitting a recommendation without a reason.
- Deciding a trend from a single measurement (needs ≥ 2).

## Suggested Implementation Order

1. Define conservative thresholds (document them) and the rule evaluator.
2. Implement the four cases → `Recommendation`s.
3. Handle insufficient-data (< 2 measurements).
4. Unit-test each rule (message + reason + severity), including thresholds.

## Validation

Run `./gradlew test` from `backend/`.
