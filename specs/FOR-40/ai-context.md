# FOR-40 AI Context

## Story

FOR-40 — Create weekly check-in model
(https://dbhlab.atlassian.net/browse/FOR-40)

## Intent

Give the Insights engine its input: a weekly snapshot combining latest body
values and training completion, built from existing summaries. Success is a
framework-free `WeeklyCheckIn` that assembles from FOR-21/FOR-28 and degrades
gracefully when data is missing.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Insights → WeeklyCheckIn)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-21/` (WeeklyBodySummary — body source),
  `specs/FOR-28/` (WeeklyTrainingSummary — training source)
- Jira: https://dbhlab.atlassian.net/browse/FOR-40

## Domain Notes

- Reuse the EXISTING summaries: `WeeklyBodySummary` (FOR-21) provides latest
  weight/body-fat/lean-mass (+ weekly deltas); `WeeklyTrainingSummary` (FOR-28)
  provides planned/completed running & strength counts. Do not recompute them.
- Both already return `null`/zero for missing data — carry that through, don't
  fabricate.
- This is the Insights `WeeklyCheckIn`, not the Body/Training summaries.

## Architectural Constraints

- `WeeklyCheckIn` type in `.../domain/`, framework-free (ADR-001).
- The assembler that reads `WeeklyBodySummaryService` + `WeeklyTrainingSummaryService`
  is an application service (FOR-21/FOR-28 service pattern), not a controller.
- No persistence.

## Common Pitfalls

- Recomputing body/training metrics instead of reading FOR-21/FOR-28.
- Fabricating values when data is missing (fake precision).
- Pulling in nutrition/shopping data (out of scope this iteration).

## Suggested Implementation Order

1. Define the `WeeklyCheckIn` record (nullable body values, session counts,
   notes).
2. Add an application builder assembling it from the FOR-21/FOR-28 services.
3. Handle the missing-data cases (null body, zero/absent training).
4. Unit-test check-in creation from summaries, including partial data.

## Validation

Run `./gradlew test` from `backend/`.
