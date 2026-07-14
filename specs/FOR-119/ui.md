# FOR-119 UI Spec

## Screens

- Configuración / Ajustes (`frontend/src/pages/SettingsPage.tsx`) at
  `/ajustes` — `ProfileSection`, `UnitsSection`, and a new training/
  nutrition preferences entry point.

## Components

- `ProfileSection` — gains a real edit form (fields: name, email,
  birthDate, sex, heightCm, activityLevel, mainGoal), replacing the
  disabled button + "Próximamente" badge.
- `UnitsSection` — gains a real selector control (or a persisted-value
  display, per the Open Question in `spec.md`), replacing the
  non-interactive `SettingsRow` list.
- New training/nutrition preference entry point (component TBD during
  implementation — could be its own section or a distinct sub-area of
  `ObjectivesSection`).

## States

- Loading: skeleton/`LoadingState` while `GET /api/v1/profile` is pending
  (FOR-60).
- Empty: N/A — profile always has defaults per FOR-107.
- Error: `ErrorState` with retry on fetch failure; inline field errors on
  save failure (FOR-61).
- Success: edited values persist and re-render; save feedback via FOR-63
  (`useNotify().success()` or `SavedIndicator`).

## Interactions

- "Editar perfil" opens an editable form (inline or modal, consistent with
  the app's existing edit patterns, e.g. `ShoppingPage`'s
  `ProductEditModal`); "Guardar" persists; "Cancelar"/close discards
  unsaved changes.
- Units selector change persists immediately or via an explicit save,
  consistent with whichever pattern is chosen (document in
  `ai-context.md` during implementation).

## Accessibility

- Form fields labelled, validation errors close to fields (ADR-006 rule:
  "Forms must display validation errors close to fields").
- Keyboard-operable throughout; visible focus (FOR-61).

## Responsive Behavior

- Edit form and new entry points follow `SettingsPage`'s existing
  mobile-first, single-column-on-narrow-viewport pattern.
