# FOR-110 AI Context

## Story

FOR-110 — [STUB] Insight history persistence + week-over-week deltas
(https://dbhlab.atlassian.net/browse/FOR-110)

## Intent

Close two gaps that `InsightsSection.tsx`'s own doc comment already
documents: "No persisted insight history exists" and "there is no
week-over-week 'delta' field on `WeeklyCheckIn`". This unlocks FOR-124's
history view and trend display without the frontend inventing either
history or deltas client-side.

## Blocked by

None. This story blocks: FOR-124.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-001-architecture.md` (framework-free domain, adapters own
  persistence)
- `docs/adr/ADR-003-persistence.md` (migration-driven schema)
- `docs/adr/ADR-005-api-design.md` (DTOs distinct from domain)
- `specs/FOR-56/spec.md` (documents the "no history endpoint" gap this
  story closes)
- `frontend/src/pages/progress/InsightsSection.tsx` (doc comment explicitly
  documenting both gaps — read it before implementing)
- Jira: https://dbhlab.atlassian.net/browse/FOR-110

## Domain Notes

- `backend/src/main/java/dev/diegobarrioh/forma/application/
  WeeklyInsights.java` — record `(checkIn, main, secondary, generatedAt)`,
  currently ephemeral (not persisted).
- `backend/.../application/WeeklyInsightsService.java` — the on-demand
  compute path to extend with persistence.
- `backend/.../delivery/insights/` package (`InsightsController`,
  `WeeklyInsightsResponse`) — the existing FOR-45/FOR-56 delivery slice to
  extend with a history route.
- `domain/WeeklyCheckIn` and `domain/Recommendation` are the FOR-40/FOR-45
  types whose snapshot needs to be persisted (not just the recommendation
  text) so deltas can be computed later.

## Architectural Constraints

- Persistence is an adapter behind the application boundary (ADR-001) — do
  not let insight-history storage leak into the domain recommendation
  rules.
- History and delta computation both read from the persisted store, not
  from re-running FOR-45's recommendation engine retroactively.
- DTOs distinct from domain; thin controllers (ADR-005).

## Common Pitfalls

- Persisting only the recommendation text and not the underlying
  `WeeklyCheckIn` snapshot — deltas need the raw signals, not just the
  message.
- Computing a delta against "zero" instead of `null` when there's no prior
  period — the frontend must be able to distinguish "no change" from "no
  data to compare."
- Breaking the existing current-week FOR-45/FOR-56 response shape while
  adding delta fields.

## Suggested Implementation Order

1. Insight-history persistence adapter + migration (period-keyed).
2. Wire persistence into the existing generation path in
   `WeeklyInsightsService`.
3. History use case + `GET /api/v1/insights/history` endpoint.
4. Delta computation (current vs. prior persisted `checkIn`) + surface on
   the current-week response.
5. Tests per `tests.md`; confirm FOR-45/FOR-56 regression coverage.

## Validation

Backend build + tests (AGENTS.md Verification guidance). Generate insights
across two or more weeks in a local/dev environment and confirm history and
deltas both reflect the persisted periods correctly.
