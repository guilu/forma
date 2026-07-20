# FOR-164 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-164
Epic: FOR-162 Design System v2. Blocked by FOR-163 (tokens); blocks the four per-view stories (FOR-165..168).

## Summary

Reconcile the existing shared components with the visual patterns in the HTML templates, consuming the
reconciled tokens (FOR-163). Update the components once so every feature page inherits the new look.
Frontend-only.

## Repository baseline (verified)

- Shared components in `frontend/src/components/` (CSS Modules): `Card`, `MetricCard`, `StatusPill`,
  `Badge`, `Button`, `ChartContainer`, `LineChart`, `MacroRing`, `FormField`, `Modal`, `ConfirmDialog`,
  plus FOR-60 states (`LoadingState`/`EmptyState`/`ErrorState`/`WidgetLoading`/`PermissionErrorState`),
  `NotificationProvider` (FOR-63), `Icon`, `ThemeToggle`, `Brand`.
- `DesignSystemExamples.tsx` — the showcase used for visual/a11y review.
- `Card` already carries `headingLevel` (FOR-112). FOR-61 established a11y patterns; FOR-60 the state components.

## Template patterns to fold in

- Card elevation / stroke / radius (`surface-elevated`, `surface-stroke`), badge & status pill styling,
  button variants, chart-container framing, metric-card typography — as shown across `docs/*.html`.
- Material Symbols iconography (via `Icon`) if not wired in FOR-163.

## Functional Requirements

- Update `Card`/`MetricCard`, `Badge`/`StatusPill`, `Button`, `ChartContainer`/`LineChart`/`MacroRing`
  to match the template patterns using tokens only (no hardcoded colors/spacing; no per-page overrides).
- Preserve component APIs (props/behaviour), including `Card` `headingLevel`; extend props only when a
  template introduces a genuinely new variant.
- Keep FOR-60 state components and FOR-63 notifications visually consistent with the refreshed look.
- Preserve accessibility (FOR-61): visible focus, contrast, semantics.

## Non-Functional Requirements

- Token-driven styling (FOR-163); both themes render correctly.
- No CDN Tailwind/fonts (inherit FOR-163 decisions).
- `DesignSystemExamples` updated to demonstrate the refreshed components.

## UI / States (see ui.md)

- Component-level restyle; feature pages unchanged here (they follow in FOR-165..168).

## Edge Cases

- A template variant with no current prop → add a prop/variant with a safe default (don't break call sites).
- Components consumed with per-page CSS-module overrides → move the visual rule into the component/token,
  remove the override (or flag it for the per-view story).
- Snapshot churn — intentional; review diffs rather than blindly re-baselining.

## Open Questions

- Whether new variants (e.g. an "elevated" card, a new badge tone) are added now or per-view as needed —
  default: add shared variants here, consume them in FOR-165..168.
- Icon system: confirm `Icon`/Material Symbols wiring lands here vs FOR-163.
