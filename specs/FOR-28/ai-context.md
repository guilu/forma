# FOR-28 AI Context

## Story

FOR-28 — Create weekly training summary
(https://dbhlab.atlassian.net/browse/FOR-28)

## Intent

Turn the training plan + completion status into a simple weekly adherence
summary the dashboard can show (running/strength planned vs. completed,
distances). Success is a deterministic, honest, rule-based summary with a clear
empty state — mirroring the FOR-21 body summary.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `docs/ui-guidelines.md` (calm, factual tone)
- `specs/FOR-21/` (WeeklyBodySummary — the pattern to follow)
- `specs/FOR-22/`, `specs/FOR-23/`, `specs/FOR-25/`, `specs/FOR-27/`
- Jira: https://dbhlab.atlassian.net/browse/FOR-28

## Domain Notes

- Counts + sums only; no forecasting. Completed distance uses only completed
  sessions.
- This is a Training-context summary, distinct from the Insights engine — no
  recommendations here.

## Architectural Constraints

- Domain/application layer computation reading via a repository/port (like
  FOR-21 `WeeklyBodySummaryService`), not a controller.
- No new persisted entity; computed on demand.
- No HTTP endpoint required by this story (see spec.md Open Questions).

## Common Pitfalls

- Reporting a fabricated completed distance when completion data is missing.
- Zero-filling an empty week instead of a clear empty-state result.
- Building the Insights engine / recommendations (out of scope).
- Putting the calc in a controller.

## Suggested Implementation Order

1. Define the summary value type (counts + distances + empty flag/message).
2. Implement the pure calc over the week's planned sessions + statuses.
3. Wire an application service reading via the repository/port.
4. Tests: happy path (mixed completion), empty week, missing-distance case.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend" row).
