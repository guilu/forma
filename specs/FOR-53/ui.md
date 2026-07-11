# FOR-53 UI Spec

## Screens

- Entrenamiento (`frontend/src/pages/TrainingPage.tsx`) at `/entrenamiento`.
  Mockup: `docs/3-entrenamiento.png`.

## Components

- Today's session card ("ENTRENAMIENTO DE HOY") with focus + completion ring.
- Weekly calendar (Mon–Sun) with status per session.
- Session detail (running / strength) + exercise list (series/reps/peso/descanso/
  estado) with a completion control.
- Weekly summary tiles (planned vs completed; volume/duration if backed).
- Reuse FOR-50 status badges + `Card`; muscle-map/streak/calories deferred
  unless backed.

## States

- Loading: calendar/session skeletons (FOR-60).
- Empty: no plan this week → clear empty state.
- Error: load or completion failure → error + retry; prior status preserved.
- Success: week + today's session with live statuses.

## Interactions

- Open a day/session → detail.
- "Iniciar/marcar entrenamiento" or per-session complete → FOR-27 PATCH; status
  updates in calendar + summary. Reversible only as far as the backend allows.

## Accessibility

- Calendar days and session controls are keyboard-operable with visible focus.
- Status conveyed by text/icon, not color alone; completion buttons labelled.

## Responsive Behavior

- Desktop: calendar/detail + side summary column.
- Mobile: today's session first, then exercise list; "mark completed" easy to
  reach (mobile priority); no horizontal scroll.
