# FOR-158 Test Plan

Strict TDD: failing tests first. Mock the training client — no real backend.

## Scope

The `/training/workouts/{type}` client, the day→`WorkoutType` mapping, and the per-exercise render
(all `repScheme` branches) in the strength detail. FOR-154 backend is done and out of scope.

## API client (`training.ts`)

- `getWorkoutTemplate('PUSH'|'PULL'|'LEGS')` → `GET /api/v1/training/workouts/{type}`, returns 5
  exercises with `exerciseName`, `sets`, `repScheme`, `repsMin/Max`, `durationSecondsMin/Max`, `rir`, `restSeconds`.
- Error (e.g. 400 bad type) surfaces as a rejected promise.

## Day → WorkoutType mapping

- A strength session resolves to the correct `WorkoutType` from the week read model (assert the
  mapping, not a hardcoded weekday table).

## Strength breakdown render

- **RANGE** exercise → "8-12 reps" (from `repsMin`/`repsMax`).
- **AMRAP** exercise → "AMRAP" (no numeric reps).
- **TIME** exercise → "45-75 s" (from `durationSeconds*`); equal min/max → single "45 s".
- RIR and rest rendered when present; omitted gracefully when null.
- Exactly 5 exercises render for a block.
- Placeholder text is gone once data loads.

## States

- Loading → `LoadingState`; template error → `ErrorState` scoped to the breakdown (header intact);
  unmapped type → readable empty/error (FOR-60).
- Running/rest session detail unchanged (regression guard).

## Accessibility

- Loading `role="status"`, error `role="alert"`; axe coverage extended.

## Fixtures

- Mocked template payloads for PUSH/PULL/LEGS including RANGE, AMRAP and TIME exercises, plus an error case.
