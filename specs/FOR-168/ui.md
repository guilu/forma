# FOR-168 UI Spec

## Screens

- Nutrición (`frontend/src/pages/NutritionPage.tsx`), route `/nutricion`. Template: `docs/4-nutricion.html`.

## Components

- Day-type selector, daily meal plan, meal detail cards, macro summary (`MacroRing`) — restyled via
  FOR-164 + FOR-163 tokens.

## States

- Selector + meal plan + meal cards + macro summary — success state to the template.
- Loading → `LoadingState`; empty → `EmptyState`; error → `ErrorState` (FOR-60).
- Day-type guidance (running-day / light-dinner notes) legible per selected day.

## Interactions

- Selecting a day type switches the plan (behaviour unchanged).
- Navigation to shopping-list generation (if present) unchanged.

## Accessibility

- Selector keyboard-operable + labelled; macro summary values as text (not color alone); contrast both themes.

## Responsive Behavior

- Mobile single-column: selector, meal plan and macro summary stack; cards full-width.
