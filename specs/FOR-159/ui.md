# FOR-159 UI Spec

## Screens

- Weekly tracking section (new) — under Progreso or Mediciones (`frontend/src/pages/`; confirm
  placement against `docs/6-progreso.png` / `docs/2-mediciones.png`).

## Components

- New: weekly-tracking history list + add/edit form + delete-with-confirm.
- Reuse `Card` (`headingLevel`, FOR-112), FOR-60 `LoadingState`/`EmptyState`/`ErrorState`,
  `useNotify` toasts (FOR-63), the existing form field + confirmation-dialog patterns.

## States

- **Empty**: no weeks recorded → `EmptyState` with a call to add the first week (normal state).
- **Loading**: history fetch in flight (`LoadingState`).
- **Success**: list of weeks; each row shows week, date, weight, body-fat %, BMI, km, pace (mm:ss),
  recommended kcal, comment; derived fat/lean mass from the backend.
- **Add/Edit**: form with the fields above; edit pre-fills and re-POSTs (upsert).
- **Delete**: explicit confirmation dialog → success toast; 404 → readable error.
- **Error**: fetch failed → `ErrorState` with retry.

## Interactions

- Add → form → POST → row appears.
- Edit a row → same form pre-filled → POST (upsert) → row updates, no duplicate.
- Delete a row → confirm → DELETE → row removed + toast.
- Body-fat source is labelled per the coherence decision (what feeds insights vs measurements).

## Accessibility

- Labelled fields; validation errors announced and tied to their field.
- Destructive delete is keyboard-operable with an explicit confirm; not color-only.
- State changes announced (`role="status"` / `role="alert"`), consistent with FOR-60.

## Responsive Behavior

- Mobile single-column: history rows stack/scroll; the form is full-width; matches the
  Progreso/Mediciones responsive pattern.
