# FOR-124 AI Context

## Story

FOR-124 — Progress: insights history view + WoW deltas
(https://dbhlab.atlassian.net/browse/FOR-124)

## Intent

`InsightsSection.tsx`'s doc comment documents both gaps this story closes,
verbatim: no persisted history ("a 'historical insights list' is deferred
entirely rather than built against nothing") and no delta field ("this
shows the signals the recommendation was computed from, not a trend").
FOR-110 ships both; this story is the frontend consumption.

## Blocked by

FOR-110 (backend: insight history persistence + WoW delta fields). Do not
start until FOR-110's history endpoint and delta fields exist.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (render read models, no derived domain
  math)
- `specs/FOR-110/spec.md` (the API surface this story consumes)
- `specs/FOR-56/spec.md` (the current-week insights story this extends;
  read its Open Question about no dedicated nav item — this story
  preserves that decision unless there's a strong reason not to)
- Jira: https://dbhlab.atlassian.net/browse/FOR-124

## Domain Notes

- `frontend/src/pages/progress/InsightsSection.tsx` — read the full doc
  comment; it names both gaps this story closes and explains why history
  and deltas were deliberately deferred rather than faked.
- `frontend/src/api/insights.ts` — `WeeklyCheckIn`/`WeeklyInsights` types
  to extend with delta fields and a history-list type.
- `StatusPill` (FOR-50) — already reused for severity in the current-week
  view; reuse identically in the history view for consistency.

## Architectural Constraints

- No client-side delta computation or history reconstruction — FOR-110's
  response is the sole source for both (architecture-overview.md,
  mirrors the existing `BodyWidget` precedent cited in
  `InsightsSection.tsx`'s own doc comment: "rendering only what the API
  actually returns instead of recomputing a comparison in the UI").
- Preserve FOR-56's "no new nav item" decision unless the implementation
  finds it doesn't fit; document any deviation.

## Common Pitfalls

- Computing a "delta" client-side from two separately-fetched current/
  historical payloads instead of using FOR-110's backend-computed delta
  field — this duplicates domain logic in the UI (forbidden by ADR-006)
  and risks drifting from the backend's comparison rules (e.g. gap-week
  handling, per FOR-110's spec).
- Showing a fabricated zero or dash for a missing delta instead of simply
  omitting it, same as today's precedent for absent data.
- Building a heavy new navigation structure for history instead of
  extending Progreso, contradicting FOR-56's existing, deliberate
  decision.

## Suggested Implementation Order

1. Confirm FOR-110 has shipped; extend `insights.ts` types.
2. Add delta rendering to related signals in `InsightsSection`.
3. Build the history list/view consuming the new history endpoint.
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise Progreso against a local/dev backend with at least
two weeks of generated insights once FOR-110 is available, confirming both
deltas and history render correctly.
