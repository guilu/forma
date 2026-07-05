# FOR-21 AI Context

## Story

FOR-21 — Create weekly body check-in summary (backend)
(https://dbhlab.atlassian.net/browse/FOR-21)

## Intent

Turn raw `BodyMeasurement` history (FOR-15/FOR-16) into a simple, honest
weekly summary the dashboard can eventually surface. Success is a rule-based
calculation that handles missing data gracefully and never overstates
precision, matching docs/ui-guidelines.md's calm, non-gamified tone.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md`
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/`, `specs/FOR-16/` (domain model and persistence this
  summary reads)
- Jira: https://dbhlab.atlassian.net/browse/FOR-21

## Domain Notes

- This is a Body-context summary over `BodyMeasurement` data, distinct from
  the Insights-context `WeeklyCheckIn`/`Recommendation` concepts in
  docs/domain-model.md. Do not conflate the two or implement the full
  Insights engine here.
- The example status-message tone in docs/ui-guidelines.md ("Body fat is
  down 0.3% over the last 2 weeks while weight is stable. Keep calories
  unchanged this week.") is Insights-engine territory (it includes a
  recommendation); this story's message should stay factual/descriptive
  (state what changed) without prescribing an action, since recommendations
  are a separate concern.

## Architectural Constraints

- Implement the calculation in the domain/application layer
  (`backend/src/main/java/dev/diegobarrioh/forma/domain/` or
  `application/`), reading through the FOR-16 repository — not in a
  controller.
- No new HTTP endpoint is required by this story; if the dashboard needs one
  later, that is a separate/explicit decision (see spec.md Open Questions).
- No new persisted entity for the summary unless a later story asks for
  historical snapshots.

## Common Pitfalls

- Reporting a weekly change as `0` when there simply isn't a prior
  measurement, instead of omitting it / saying data is insufficient.
- Writing a "gamified" or prescriptive status message — docs/ui-guidelines.md
  explicitly rejects confetti/guilt/streak language; keep the message
  factual.
- Building the full Insights `Recommendation` engine when this story only
  asks for a Body-context summary.

## Suggested Implementation Order

1. Implement latest-value extraction (weight, body fat %, lean mass) from
   the most recent measurement.
2. Implement the weekly-change calculation with a documented "prior
   measurement" matching rule.
3. Implement the insufficient-data path (status message + null/absent
   change fields).
4. Add backend tests covering both the happy path and insufficient-data
   cases.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md Verification guidance,
"Backend" row).
