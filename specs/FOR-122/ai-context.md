# FOR-122 AI Context

## Story

FOR-122 — Objetivos screen
(https://dbhlab.atlassian.net/browse/FOR-122)

## Intent

FOR-58's own spec flagged this gap explicitly: "The 'Objetivo principal /
objetivos' here overlaps the Objetivos screen (`docs/7-objetivos.png`),
which has no dedicated FOR-47 child — treat goals as read-only summary here
and flag the gap." This story is that flagged gap being picked up: a real
Objetivos screen, not just a summary row.

## Blocked by

FOR-104 (progress & goals domain backend — external to this batch; **no
spec folder exists for it in this repository as of this story's
authoring**, verified via `ls specs/FOR-104` returning nothing). Do not
start UI implementation against invented endpoints — confirm FOR-104's
actual spec/API exists first (AGENTS.md: "Repository state has priority
over roadmap/spec intent... do not invent missing code").

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md`
- `specs/FOR-58/spec.md` (documents this exact gap in its Data Model Notes
  and Open Questions)
- `docs/7-objetivos.png` (mockup)
- FOR-104's spec, once it exists (not present in this repository at the
  time this spec was written)
- Jira: https://dbhlab.atlassian.net/browse/FOR-122, and
  https://dbhlab.atlassian.net/browse/FOR-104 for the blocking backend

## Domain Notes

- `frontend/src/pages/GoalsPage.tsx` — currently a one-line
  `PagePlaceholder`, verified directly against the source.
- `frontend/src/app/routes.tsx` line 37 and `frontend/src/app/
  navigation.ts` already register `/objetivos` → `GoalsPage` with a
  "goals" icon and `Objetivos` label — routing/nav plumbing is done;
  only the page content is missing.
- `frontend/src/pages/settings/ProfileSection.tsx` lines ~86–91 — the
  existing read-only "Objetivo principal" row, whose own comment says
  "goals ('Objetivos') have no dedicated owning story/backend yet;
  documented gap, not built here" — this story is that story.
- `frontend/src/pages/settings/ObjectivesSection.tsx` — inert
  "Objetivos por defecto" rows with the same documented gap in its doc
  comment.

## Architectural Constraints

- No goal-progress computation client-side — render whatever FOR-104's
  read model returns (architecture-overview.md).
- This story's scope is bounded by whatever FOR-104 actually ships; do not
  expand scope to compensate for a narrower-than-expected backend — cut
  the UI scope instead and document the reduction.

## Common Pitfalls

- Building against a guessed FOR-104 API shape before it exists — this is
  the single biggest risk in this story. Confirm the real contract first.
- Leaving `ProfileSection`/`ObjectivesSection`'s stale gap-documentation
  comments in place after this story ships a real screen — update them to
  reference the new Objetivos screen instead of describing a gap that no
  longer exists.

## Suggested Implementation Order

1. Confirm FOR-104's actual spec/API exists; read it before writing any
   frontend code.
2. Build goal list + loading/empty/error states.
3. Build goal detail view.
4. Build edit flow if FOR-104 exposes an update command.
5. Update `ProfileSection`/`ObjectivesSection` to link through instead of
   duplicating stale gap documentation.
6. Tests per `tests.md`, against FOR-104's real contract.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/7-objetivos.png`. Do not mark this story
verifiable until FOR-104's real backend contract is exercised end to end.
