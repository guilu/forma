# FOR-158 UI Spec

## Screens

- Entrenamiento → strength-session detail (`frontend/src/pages/TrainingPage.tsx`, mockup `docs/3-entrenamiento.png`).

## Components

- Strength-session detail — placeholder replaced by a per-exercise list.
- Reuse `Card` (`headingLevel`, FOR-112), FOR-60 states, existing training-detail layout/components.

## States

- **Loading**: template fetch in flight (`LoadingState`).
- **Success**: list of 5 exercises, each showing name, sets, reps (per `repScheme`), RIR, rest:
  - RANGE → "8-12 reps"
  - AMRAP → "AMRAP"
  - TIME → "45-75 s" (single value when min == max)
- **Empty**: unmapped/unknown `WorkoutType` → readable empty message.
- **Error**: template fetch failed → `ErrorState` scoped to the breakdown; session header still shown.

## Interactions

- Opening a strength session fetches (or reads prefetched) its template and renders the breakdown.
- Running/rest sessions keep their current detail behaviour.

## Accessibility

- Exercise list is semantic (list/table); reps/RIR/rest are text, not color-coded.
- States announced (`role="status"`/`role="alert"`), consistent with FOR-60.

## Responsive Behavior

- Exercise rows stack/scroll cleanly on mobile (single-column), matching the training page pattern.
