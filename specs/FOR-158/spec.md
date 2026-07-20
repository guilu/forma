# FOR-158 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-158
Epic: FOR-47 UI & UX
Backend: FOR-154 (strength workout templates). Frontend personalization batch.

## Summary

Replace the placeholder in the strength-session detail with the real per-exercise breakdown from
the workout template catalog (FOR-154). Today the strength detail shows a placeholder — _"El
desglose por ejercicio (series, reps, peso, descanso) no está disponible todavía…"_. The backend
already exposes `GET /api/v1/training/workouts/{type}` (PUSH/PULL/LEGS) with 5 exercises per block:
`exerciseName`, `sets`, `repScheme`, `repsMin`/`repsMax`, `durationSecondsMin`/`durationSecondsMax`,
`rir`, `restSeconds`. This is the largest story in the batch — the entire breakdown is placeholder
today, not just one render branch. Frontend-only.

## Repository baseline (verify before coding)

- `frontend/src/pages/TrainingPage.tsx` — strength-session detail renders the placeholder text; no
  per-exercise list.
- `frontend/src/api/training.ts` — confirm whether a client for `/training/workouts/{type}` exists;
  add one if not.
- Backend `GET /api/v1/training/workouts/{type}` (FOR-154), `type` ∈ {PUSH, PULL, LEGS}, returns
  5 exercises/block with the fields above. Confirm the exact response shape against FOR-154.

## Day → WorkoutType mapping

- A strength session in the week must map to its `WorkoutType` (PUSH/PULL/LEGS) to fetch the right
  template. Derive the mapping from the week read model (the session already carries or implies its
  type). Document the mapping source; do NOT hardcode a weekday→type table in the UI if the week
  model provides it.

## repScheme rendering (all branches)

- **RANGE** → `"{repsMin}-{repsMax} reps"` (e.g. "8-12 reps").
- **AMRAP** → "AMRAP" (e.g. Flexiones, Dominadas).
- **TIME** → `"{durationSecondsMin}-{durationSecondsMax} s"` (e.g. "45-75 s", Plancha), from `durationSeconds*`.
- Plus RIR (`rir`) and rest (`restSeconds`) per exercise.

## Functional Requirements

- Fetch the template for the session's `WorkoutType` and render the 5 exercises: name, sets, reps
  (per `repScheme` above), RIR, rest.
- No progression/programming logic in the UI (ADR-001) — render the template read model as given.
- Keep running/rest session detail behaviour unchanged.

## Non-Functional Requirements

- Loading / empty / error states via FOR-60 shared components.
- Token-driven styling; reuse existing training detail components.

## UI / States (see ui.md)

- Per-exercise list replacing the placeholder, with all three `repScheme` renderings.

## Edge Cases

- Unknown/unmapped `WorkoutType` for a session → readable empty/error, not a crash.
- Template endpoint error → `ErrorState` scoped to the breakdown, session header still shown.
- Missing optional fields (`rir`/`restSeconds` null) → omit that detail gracefully.
- `durationSecondsMin == durationSecondsMax` (TIME) → render a single value ("45 s"), not a range.

## Open Questions

- Exact source of the day→`WorkoutType` mapping in the week read model (confirm in design).
- Whether weight is shown at all (the story lists "peso" in the placeholder, but the template fields
  are sets/reps/duration/RIR/rest — no weight field). Default: do NOT invent a weight column.
- Whether the template is fetched per session-detail open or prefetched with the week.
