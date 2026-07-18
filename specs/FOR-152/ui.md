# FOR-152 UI Spec

> Scope is limited to the dashboard cost-threshold signal on the existing `ShoppingWidget`. No new
> screen. UI renders the backend budget read model (ADR-006) — no threshold logic in the component.

## Screens

- Dashboard → `ShoppingWidget` ("Presupuesto de la compra"), existing weekly + monthly tiles.

## Components

- `frontend/src/pages/dashboard/ShoppingWidget.tsx` — add a threshold indicator next to the weekly tile:
  a state chip/label reading OK (under 120 €) or over-budget (≥ 120 €), driven by the backend
  `overThreshold` / `weeklyThresholdEur` fields (mirrors the Excel Dashboard "Coste compra semanal … <120 €/sem … OK").

## States

- Loading — existing `WidgetLoading`.
- Empty — existing empty state (no list generated).
- Error — existing `ErrorState`.
- Ready, under threshold — weekly total + "OK / dentro de 120 €" signal.
- Ready, over threshold — weekly total + "por encima de 120 €" warning styling.

## Interactions

- Read-only widget; links to `/lista-compra` (existing). The signal reflects the backend value; no client calculation.

## Accessibility

- The over/under state must be conveyed by text/label, not color alone.
- Keep existing keyboard/focus behavior of the widget link; the chip is non-interactive text.

## Responsive Behavior

- Reuse the existing widget tile layout; the signal wraps below the weekly value on narrow viewports without overflow.
