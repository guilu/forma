# FOR-125 AI Context

## Story

FOR-125 — Goals & milestones domain, persistence and API. First implementable slice (1 of 6) of FOR-104 [STUB] Progress & goals domain. Blocks FOR-122 (Objetivos screen).

## Intent

Deliver the smallest, highest-value part of the progress/goals domain: goals with milestones and derived progress. Success = the Objetivos screen (FOR-122) has a real backend to read/create/update goals against. Adherence, streaks, achievements, progress photos, and the muscle-worked map are explicitly deferred to later FOR-104 slices.

## Relevant Documents

- `specs/FOR-104/` — full progress/goals scope, slicing, and open questions (this story is slice 1).
- `AGENTS.md` — hexagonal boundaries, owner-scoping, no speculative abstractions.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`.
- Mockup: `docs/7-objetivos.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-125

## Domain Notes

- **No goals domain exists today** — this slice introduces `Goal` + `Milestone`.
- Reuse existing domain for progress derivation: `BodyMeasurement`, `WeeklyCheckIn`, `WeeklyBodySummary`. Verify their real field names before mapping `metric` → source; do not duplicate their math.
- Progress is a derived read-model concern, not a stored column.
- Follow the exact structure of the merged backend enablers: FOR-107 (`V8__user_profile.sql`, controller/DTOs/adapter), FOR-110 (read-model + persistence). Match naming, layering, and test style.

## Architectural Constraints

- Domain framework-free; application port + service; thin controller under `delivery/` (new `goals` package, mirror `delivery/profile`); JDBC adapter under `adapter/persistence`.
- Owner-scoped per ADR-002; never bypass the boundary.
- New migration is **V11** (current head is V10); one column per statement (H2/PostgreSQL convention — see V6/V7/V9).
- No out-of-scope abstractions for future FOR-104 slices.

## Common Pitfalls

- Fabricating progress for a metric with no linked data — return null/undefined instead.
- Duplicating `BodyMeasurement`/`WeeklyCheckIn` math instead of reusing it for derivation.
- Returning 404 for an empty goal list instead of an empty array.
- Coercing an unknown `metric` enum instead of 400.
- Building adherence/streak/achievements here — those are separate slices.
- Bypassing the owner boundary "because there's only one user".

## Suggested Implementation Order

1. `Goal` + `Milestone` domain (+ tests) — construction/validation/progress derivation from an injected source value.
2. Application port + service (create/read/update, owner-scoped).
3. JDBC adapter + `V11` migration (+ persistence round-trip test).
4. `delivery/goals` controller + DTOs (+ API tests) per `api.md`.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: goals persist with milestones; progress derives from linked data and is null when unlinked; empty list returns 200 `[]`; invalid metric/target → 400; unknown id PATCH → 404. Then FOR-122 (frontend) can consume `GET/POST/PATCH /api/v1/goals`.
