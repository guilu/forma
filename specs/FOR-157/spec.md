# FOR-157 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-157
Epic: FOR-47 UI & UX
Backend: FOR-152 (weekly cost threshold on the shopping read model). Corresponds to the deferred `ui.md` of FOR-152. Frontend personalization batch.

## Summary

Signal on the dashboard whether the weekly shopping cost exceeds the target threshold
(> 120 €/week). FOR-152 already exposes `weeklyThresholdEur` (120) and `overThreshold` (bool)
on the shopping read model; `ShoppingWidget` shows `budget.weeklyEur`/`budget.monthlyEur` but
does not compare against the threshold. Real catalog cost ≈ 104 €/week (under threshold).
Frontend-only.

## Repository baseline (verify before coding)

- `frontend/src/pages/dashboard/ShoppingWidget.tsx` — renders `budget.weeklyEur` +
  `budget.monthlyEur`; does NOT read the threshold fields.
- `frontend/src/api/shopping.ts` — confirm the read model already carries `weeklyThresholdEur`
  and `overThreshold` (FOR-152); extend the type if missing.

## User/System Flow

1. Dashboard loads → `ShoppingWidget` fetches the shopping read model.
2. Widget reads `overThreshold` and `weeklyThresholdEur` and renders an OK / over-budget chip
   plus the threshold label (e.g. "objetivo <120 €/sem").

## Functional Requirements

- Render a status chip/indicator in `ShoppingWidget` driven by `overThreshold`:
  under-threshold → neutral/OK; over-threshold → warning.
- Show the threshold value (`weeklyThresholdEur`) as context (e.g. "objetivo <120 €/sem").
- Backend-computed values only (ADR-001/006) — do NOT recompute the comparison in the UI; render
  `overThreshold` as given.
- Preserve the existing weekly/monthly cost display.

## Non-Functional Requirements

- Loading / empty / error states via FOR-60 shared components.
- Status not conveyed by color alone — include text/icon with an accessible label.
- Token-driven styling.

## UI / States (see ui.md)

- OK chip vs over-budget chip; threshold label always shown.

## Edge Cases

- Read model without the threshold fields (older payload) → hide the chip gracefully, keep the
  cost display; document the fallback.
- `overThreshold` true but weekly cost missing → still show the warning if the flag is present.
- Exactly at threshold → follow the backend's `overThreshold` flag (do not re-decide the boundary in UI).

## Open Questions

- Chip copy/wording ("Dentro de presupuesto" / "Sobre presupuesto") and placement within the widget.
- Whether to also surface the threshold on the full Shopping page or only the dashboard widget
  (this story scopes the dashboard widget).
