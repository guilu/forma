# FOR-53 AI Context

## Story

FOR-53 — Create training plan screens
(https://dbhlab.atlassian.net/browse/FOR-53)

## Intent

Let users see what to train today, what's next and how the week is going. Success
is a weekly calendar + session detail + exercise list + a completion action +
weekly summary, working for running, strength and rest days.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`, `docs/3-entrenamiento.png` (mockup)
- `docs/api/training-week.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-24/`/`FOR-25/` (exercises/workouts), `specs/FOR-26/`/`FOR-27/`
  (calendar/status), `specs/FOR-28/` (summary)
- Jira: https://dbhlab.atlassian.net/browse/FOR-53

## Domain Notes

- `frontend/src/pages/TrainingPage.tsx`, `api/training.ts` exist (FOR-26/27) —
  extend to the mockup, don't recreate.
- Completion is **session-level** today (FOR-27 PATCH status). The mockup's
  per-exercise logging/completion, muscle heatmap, streak and calories are not
  backed yet.

## Architectural Constraints

- Consume Training read models via `api/training.ts`; completion via the FOR-27
  PATCH. No progression rules in the UI. Reuse FOR-50 status/timeline primitives.

## Common Pitfalls

- Embedding progression/scheduling logic in the UI.
- Assuming exercise-level completion exists (it's session-level).
- Breaking rest days / empty-week states.

## Suggested Implementation Order

1. Weekly calendar from FOR-26 with per-session status (FOR-27).
2. Today's session card + running/strength detail from FOR-25 templates.
3. Exercise list (series/reps/rest) read-only where not backed; completion action
   at the session level.
4. Weekly summary (FOR-28); empty/loading/error states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/3-entrenamiento.png`.
