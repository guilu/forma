# FOR-156 UI Spec

## Screens

- Ajustes → Objetivos (`frontend/src/pages/settings/ObjectivesSection.tsx`, within the settings
  page at `/ajustes`; mockup `docs/8-configuracion.png`).

## Components

- `ObjectivesSection` — extended to read and render `personalTargets` from the profile.
- Reuse `Card` (`headingLevel`, FOR-112), shared `LoadingState`/`EmptyState`/`ErrorState` (FOR-60),
  `useNotify` (FOR-63) for save feedback. Reuse the existing settings edit control pattern if one exists.

## States

- **Loading**: while the profile is in flight (`LoadingState`).
- **Success**: targets rendered — base kcal, target body-fat range (e.g. "12–13 %"), target weight
  range (e.g. "73–75 kg"), protein/fat/carbs grams.
- **Empty**: profile without `personalTargets` → neutral empty/default display, not an error.
- **Error**: profile fetch failed → `ErrorState` with retry.
- **Editing** (only if supported): inline edit with server-side validation errors near the field;
  success toast on save.

## Interactions

- Read-only by default. If edit is in scope, an explicit "Editar" affordance mirrors the section's
  existing pattern; save persists via the profile update command; cancel discards.

## Accessibility

- Ranges and numbers conveyed as text (not color) with units.
- Edit fields labelled; validation errors announced and associated with their field.

## Responsive Behavior

- Follows the settings page responsive pattern (mobile single-column) from `docs/8-configuracion.png`.
