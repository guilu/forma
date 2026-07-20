# FOR-157 UI Spec

## Screens

- Dashboard → `ShoppingWidget` (`frontend/src/pages/dashboard/ShoppingWidget.tsx`, mockup `docs/1-dashboard.png`).

## Components

- `ShoppingWidget` — extended with an OK / over-budget chip.
- Reuse an existing chip/`StatusPill` if available, `Card` (`headingLevel`, FOR-112), FOR-60 states.

## States

- **Under budget** (`overThreshold: false`): neutral/OK chip, e.g. "Dentro de presupuesto".
- **Over budget** (`overThreshold: true`): warning chip, e.g. "Sobre presupuesto".
- Threshold label always visible when known (e.g. "objetivo <120 €/sem"), value from `weeklyThresholdEur`.
- **Loading / error**: FOR-60 states, consistent with the other dashboard widgets.
- **No threshold data**: chip hidden; weekly/monthly cost still shown.

## Interactions

- Static indicator (no action). Navigation to the Shopping page follows the existing widget pattern.

## Accessibility

- Status conveyed by text + icon, not color alone; accessible label on the chip.

## Responsive Behavior

- Chip wraps within the widget on narrow widths; dashboard grid unchanged (mobile single-column).
