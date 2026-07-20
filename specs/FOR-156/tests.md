# FOR-156 Test Plan

Strict TDD: failing tests first, then implement. Mock `apiClient`/the profile client — never a real backend.

## Scope

Rendering `personalTargets` in `ObjectivesSection`, its loading/empty/error states, and — only if
in scope — the edit→save flow. FOR-149 backend is done and out of scope.

## API client (`profile.ts`)

- Profile fetch types `personalTargets` (base kcal, body-fat min/max, weight min/max, protein/fat/carbs).
- If edit is in scope: the update call posts the changed targets and returns the updated profile;
  a validation error (400) surfaces as a rejected promise.

## ObjectivesSection

- With a full `personalTargets`, renders every field (kcal, body-fat range, weight range, macros).
- Missing/partial targets → present fields only, no crash.
- Loading → `LoadingState`; fetch error → `ErrorState`; genuinely empty → `EmptyState` (FOR-60).
- Numbers come from the read model (assert the rendered values equal the mocked payload, not constants).

## Edit flow (only if the section supports editing)

- Editing a target and saving calls the update endpoint with the new value.
- Server validation error → field-level message, entered values preserved.
- Success → `useNotify` success toast; refreshed values shown.

## Accessibility

- Loading `role="status"`, error `role="alert"`; existing axe coverage extended.
- Field labels associated; errors announced.

## Fixtures

- Mocked profile payload with a full `personalTargets`, plus a partial one and an error case.
