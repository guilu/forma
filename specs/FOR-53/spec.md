# FOR-53: Create training plan screens

Jira: https://dbhlab.atlassian.net/browse/FOR-53
Epic: FOR-47 UI & UX

## Summary

Build the training (Entrenamiento) screens: weekly calendar, running and strength
session detail, exercise list (sets/reps/rest), a completion action and a weekly
summary. Mockup: `docs/3-entrenamiento.png`. `TrainingPage.tsx` exists
(FOR-26/27); this story aligns it to the mockup. Must work for running, strength
and rest days without embedding progression rules in the UI.

## User/System Flow

1. User opens Entrenamiento (`/entrenamiento`).
2. The weekly calendar + today's session load from the Training read models
   (FOR-26 week, FOR-25 templates, FOR-28 summary).
3. User opens a session detail and marks it completed (FOR-27 PATCH); status
   reflects across the calendar and summary.

## Functional Requirements

- **Weekly calendar**: the current week (Mon–Sun) with running/strength/rest days
  and completed-vs-pending status (reuse the FOR-26 week + FOR-27 status).
- **Today's session card**: "ENTRENAMIENTO DE HOY" with focus, estimated
  duration and a completion ring (X/Y exercises).
- **Session detail**: running session view and strength session view.
- **Exercise list**: per exercise → series, reps, peso, descanso, estado, with a
  per-exercise complete toggle (from FOR-25 workout templates).
- **Completion action**: "Iniciar/marcar entrenamiento"; explicit and only
  reversible if the backend supports it (FOR-27 allows status changes).
- **Weekly summary**: planned vs completed counts (FOR-28), volume/duration if
  backed.
- Empty (no plan), loading and error states (FOR-60).

## Non-Functional Requirements

- No progression/scheduling rules in the UI (ADR-001) — read models only.
- Reusable status + timeline/calendar components.

## Data Model Notes

Consumes FOR-26 weekly schedule, FOR-27 session status (PATCH), FOR-25 workout
templates (exercise sets/reps), FOR-28 weekly summary, and **FOR-136 muscle-worked
map** (`GET /api/v1/training/sessions/{sessionId}/muscle-map` → `{ muscle, load }`
per muscle, load ∈ HIGH/MEDIUM/LOW) for the heatmap. **Mockup extras not yet
backed**: per-exercise logged weight/rest and per-exercise completion, estimated
calories, weekly-history bars, "RACHA ACTUAL" streak — the current model has
session-level (not exercise-level) completion. Render only what the API supports;
show the rest as placeholder or omit, and do not invent backend (repository
priority, AGENTS.md). Document the exercise-level-completion gap for a future
backend story.

**Muscle-heatmap label normalization (from FOR-136):** the muscle-map endpoint
returns muscle labels verbatim from `Exercise.primaryMuscles()` — granular,
lowercase, accented Spanish (e.g. `"hombro"` AND `"hombro anterior"` are distinct
values; `"tríceps"` carries an accent). The backend does not normalize (faithful
derivation, no fabrication — repo priority). The **frontend owns a display-label /
normalization map** so the heatmap doesn't render fragmented or inconsistent muscle
groups: group `"hombro anterior"` → `"hombro"`, handle casing/accents, and map to
the mockup's muscle regions. Keep this map in the UI layer only; do not push
normalization back into the backend read model.

## Edge Cases

- Rest day → clearly shown, no session actions.
- No plan for the week → empty state.
- Marking completion fails → error, prior status preserved (FOR-27 pattern).

## Open Questions

- Per-exercise completion + logged weight/rest exceed current backend (session-
  level only) — recommend session-level completion for the MVP and document the
  exercise-level gap.
- Calories/streak: defer unless an endpoint exists.
- Muscle-map: **now backed by FOR-136** — wire the heatmap to the muscle-map
  endpoint, but own the label-normalization map in the UI (see Data Model Notes).
