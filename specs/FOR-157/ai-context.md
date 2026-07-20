# FOR-157 AI Context

## Story

FOR-157 — Dashboard: chip de umbral de coste en `ShoppingWidget` (consume FOR-152). Frontend-only.

## Intent

FOR-152 added a weekly cost threshold (120 €/sem) and an `overThreshold` flag to the shopping read
model, but the dashboard widget never surfaces it. Add a small OK / over-budget chip so the user
sees at a glance whether the weekly shop is within budget.

## Relevant Documents

- `specs/FOR-152/` — cost threshold + `overThreshold` on the shopping read model (deferred `ui.md`).
- `specs/FOR-51/` (dashboard widgets), `specs/FOR-55/` (shopping screens).
- `AGENTS.md` — frontend renders backend-computed values; no domain logic in UI.
- `docs/adr/ADR-001-domain-first.md`, `ADR-006-frontend.md`, `docs/1-dashboard.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-157

## Repo Notes (verify)

- `frontend/src/pages/dashboard/ShoppingWidget.tsx` — shows `budget.weeklyEur`/`budget.monthlyEur` only.
- `frontend/src/api/shopping.ts` — confirm `weeklyThresholdEur` + `overThreshold` are typed; extend if not.
- Reuse an existing chip/`StatusPill` component if present; FOR-60 states; `Card`/`headingLevel` (FOR-112).

## Architectural Constraints

- Frontend-only; render `overThreshold` as computed by the backend — never recompute the comparison.
- Accessible status (text + icon, not color alone).
- Token-driven styling; no hardcoded threshold number in the UI (read `weeklyThresholdEur`).

## Common Pitfalls

- Re-deriving `weeklyEur > 120` in the widget instead of reading `overThreshold`.
- Hardcoding "120 €" instead of rendering `weeklyThresholdEur`.
- Color-only status with no accessible label.
- Assuming the threshold fields always exist (older read-model payloads).

## Suggested Implementation Order

1. Confirm/extend the shopping read-model type in `shopping.ts` (+ client test).
2. Add the chip to `ShoppingWidget` driven by `overThreshold`, with the threshold label (+ test).
3. Graceful fallback when the fields are absent (+ test).

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm the chip
reflects `overThreshold`, shows the threshold from the payload, and degrades when the fields are
missing. No comparison logic in the UI.
