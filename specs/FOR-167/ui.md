# FOR-167 UI Spec

## Screens

- Entrenamiento (`frontend/src/pages/TrainingPage.tsx`), route `/entrenamiento`. Template: `docs/3-entrenamiento.html`.

## Components

- Weekly calendar, session detail, exercise breakdown, streak/weekly-history widgets (FOR-143), muscle map
  (`trainingMuscleLabels.ts` + `ChartContainer`) — restyled via FOR-164 + FOR-163 tokens.

## States

- Session detail (running / strength / rest), completion status, streak/history, muscle map — success state
  to the template.
- Loading → `LoadingState`; empty (streak zeroed, no sessions) → `EmptyState`; error → `ErrorState` (FOR-60).

## Interactions

- Mark-session-complete and navigation unchanged in behaviour.
- Coordinate with FOR-158 (breakdown) / FOR-161 (running-plan) if they land around the same time.

## Accessibility

- Calendar/session semantics; keyboard-operable completion; muscle-map + streak conveyed with text, not
  color alone; contrast both themes (FOR-61).

## Responsive Behavior

- Mobile single-column: calendar and session detail stack; muscle map/chart scroll within their containers.
