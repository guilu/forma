# FOR-119 Test Plan

## Scope

Verify profile edit, units selector and the new training/nutrition
preference entry points load real data, persist changes, and handle
loading/error states correctly.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-107 contract; no backend change in this story.

## UI Tests

- `ProfileSection` loads and displays the real profile from
  `GET /api/v1/profile` instead of `MOCK_PROFILE`.
- "Editar perfil" opens an editable form with the current values
  pre-filled.
- Saving valid changes persists them and re-renders with the new values
  plus success feedback.
- Saving with a backend validation error shows the error close to the
  relevant field, without losing the user's other entered values.
- `UnitsSection` reflects the persisted unit preference (and, if a
  selector, allows changing it and persists the change).
- A new, distinct training/nutrition preference entry point is reachable
  and separate from `ObjectivesSection`'s default-objectives rows.
- Profile fetch failure renders `ErrorState` with a working retry.

## Edge Cases

- Save while a previous save is still in flight → second save disabled/
  queued, not a race that corrupts displayed state.
- Empty/default profile (first run, before any edit) → form pre-fills with
  FOR-107's documented defaults, not blank/undefined fields.

## Fixtures

- A mocked `GET /api/v1/profile` response with realistic values and one
  representing FOR-107's first-run defaults.
- A mocked validation-error response for the save-failure test.
