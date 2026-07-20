# FOR-165 UI Spec

## Screens

- Dashboard (`frontend/src/pages/DashboardPage.tsx` + `frontend/src/pages/dashboard/*`), route `/`.
  Template: `docs/1-dashboard.html`.

## Components

- `WidgetSection`, `BodyWidget`, `TrainingWidget`, `NutritionWidget`, `ShoppingWidget`, `InsightWidget`,
  `SyncWidget`, `ProgressBar` — restyled via FOR-164 shared components + FOR-163 tokens.

## States

- Per widget: loading (`WidgetLoading`/`LoadingState`), empty (`EmptyState`), error (`ErrorState`),
  success — all preserved, restyled to the template.
- New-user empty dashboard remains a normal state.

## Interactions

- Widget → feature-page navigation unchanged.
- Visual-only refactor; no new actions.

## Accessibility

- Section headings/landmarks; keyboard-operable widget links; contrast both themes (FOR-61).

## Responsive Behavior

- Mobile single-column → desktop multi-column grid per the template breakpoints.
