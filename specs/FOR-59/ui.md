# FOR-59 UI Spec

## Screens

- First-run onboarding flow (new route/overlay, e.g. `/onboarding`). No dedicated
  mockup; goal step mirrors `docs/7-objetivos.png`.

## Components

- Step wrapper: progress indicator, title, content, back/next/skip.
- Step content: profile form, body-metrics (reuse `MeasurementForm`), goal
  selector, training-availability, equipment, nutrition-basics, integration
  prompt (FOR-57).
- Completion screen with a clear next action.
- Reuse FOR-50 primitives, FOR-60 states, FOR-63 feedback.

## States

- Loading: step transitions are instant; any async import shows a spinner.
- Empty: default first-run state.
- Error: per-step validation error near the field.
- Success: step accepted → advance; final → dashboard.

## Interactions

- Next/back navigation; skip for non-critical steps; resume where persisted.
- Goal selection sets the main objective (captured, persistence deferred).
- Integration prompt → FOR-57 connect entry point.

## Accessibility

- Steps announce progress (e.g. "Paso 2 de 7"); focus moves to the step heading.
- Form fields labelled with associated errors; controls keyboard-operable.

## Responsive Behavior

- Desktop: centered card flow.
- Mobile: full-width steps, large touch targets, one primary action per step; no
  horizontal scroll.
