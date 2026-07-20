# FOR-158 AI Context

## Story

FOR-158 — Detalle de fuerza: desglose real de ejercicios (consume FOR-154). Frontend-only. The
biggest story in the batch: the whole strength breakdown is placeholder today.

## Intent

Turn the strength-session detail from a "not available yet" placeholder into the real 5-exercise
breakdown (name, sets, reps, RIR, rest) served by the FOR-154 template catalog, rendering RANGE /
AMRAP / TIME rep schemes correctly.

## Relevant Documents

- `specs/FOR-154/` — workout template catalog + `GET /training/workouts/{type}` shape.
- `specs/FOR-53/` — training screens (where the placeholder lives).
- `AGENTS.md` — frontend renders read models; no programming/progression logic in UI.
- `docs/adr/ADR-001-domain-first.md`, `ADR-002`, `docs/3-entrenamiento.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-158

## Repo Notes (verify)

- `frontend/src/pages/TrainingPage.tsx` — placeholder string in the strength detail; replace it.
- `frontend/src/api/training.ts` — add a `/training/workouts/{type}` client if absent.
- Backend `GET /api/v1/training/workouts/{type}` (type PUSH/PULL/LEGS), 5 exercises/block:
  `exerciseName`, `sets`, `repScheme` (RANGE/AMRAP/TIME), `repsMin/Max`, `durationSecondsMin/Max`,
  `rir`, `restSeconds`.
- Reuse FOR-60 states, `Card`/`headingLevel` (FOR-112), existing training-detail layout.

## Architectural Constraints

- Frontend-only; consume the template endpoint. No progression/programming logic in the UI (ADR-001).
- Render each `repScheme` branch from the read model; do not compute reps/durations client-side.
- Accessible states; no color-only meaning.

## Common Pitfalls

- Handling only RANGE and forgetting AMRAP/TIME branches (the story flags TIME/AMRAP specifically).
- Inventing a "peso" (weight) column — the template has no weight field.
- Hardcoding a weekday→WorkoutType map instead of using the week read model's session type.
- Rendering a "0-0 reps" for TIME/AMRAP exercises because `repsMin/Max` are absent for those schemes.
- Treating `durationSecondsMin == Max` as a range.

## Suggested Implementation Order

1. Add the `/training/workouts/{type}` client + response type in `training.ts` (+ client test).
2. Resolve the session's `WorkoutType` from the week model (+ test of the mapping).
3. Render the 5-exercise breakdown with all three `repScheme` branches, RIR, rest, FOR-60 states,
   replacing the placeholder (+ component tests per branch).

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm each
`repScheme` renders correctly (RANGE "8-12 reps", AMRAP "AMRAP", TIME "45-75 s"), RIR/rest show,
running/rest details are unchanged, and error/empty states are handled. No programming logic in UI.
