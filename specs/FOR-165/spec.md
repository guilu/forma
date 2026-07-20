# FOR-165 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-165
Epic: FOR-162 Design System v2. Blocked by FOR-164 (shared components).

## Summary

Refactor the dashboard view to match `docs/1-dashboard.html`, using the reconciled tokens (FOR-163) and
refreshed shared components (FOR-164). Layout/visual only — preserve data wiring and states. Frontend-only.

## Repository baseline (verified)

- `frontend/src/pages/DashboardPage.tsx` (+ `.module.css`) composes widgets from
  `frontend/src/pages/dashboard/`: `BodyWidget`, `TrainingWidget`, `NutritionWidget`, `ShoppingWidget`,
  `InsightWidget`, `SyncWidget`, plus `WidgetSection`, `ProgressBar`, `WidgetLoading`.
- Widgets already wire data + FOR-60 loading/empty/error states and navigate to their feature pages.
- Template: `docs/1-dashboard.html` (dark, FORMA green, card grid, metric emphasis).

## Functional Requirements

- Align `DashboardPage` layout, the widget grid/sections and each widget card with the template.
- Consume FOR-164 components + FOR-163 tokens; remove per-page visual overrides in `DashboardPage.module.css`
  / widget modules where a shared component/token now covers them.
- Preserve widget data wiring, navigation, and FOR-60 states (loading/empty/error) unchanged.

## Non-Functional Requirements

- Responsive: mobile single-column → desktop grid, matching the template breakpoints.
- Both themes render (FOR-62); accessibility preserved (FOR-61).
- No hardcoded visual rules — tokens/components only.

## UI / States (see ui.md)

- Widget cards restyled; each keeps its loading/empty/error state.

## Edge Cases

- A widget whose template treatment needs a new shared variant → add it in FOR-164, consume here (don't
  hardcode locally).
- New-user empty dashboard → template-consistent empty states (FOR-60) preserved.
- Widget data/behaviour must not change — this is a visual refactor.

## Open Questions

- Exact widget order/grouping if the template differs from the current composition — follow the template,
  document deviations.
- Whether any widget needs a layout variant not yet in FOR-164 (raise it there).
