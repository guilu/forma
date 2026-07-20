# FOR-166 AI Context

## Story

FOR-166 — Mediciones view: apply mockup template layout (`docs/2-mediciones.html`). Frontend-only visual
refactor. Blocked by FOR-164.

## Intent

Restyle the measurements view to the approved template using refreshed components/tokens, preserving data,
validation and the manual/imported distinction.

## Relevant Documents

- Template `docs/2-mediciones.html`; `specs/FOR-163/`, `specs/FOR-164/`.
- `specs/FOR-52/` (body composition screens), FOR-60 (states), FOR-61 (a11y), FOR-62 (theme).
- `AGENTS.md` — no domain logic in UI.
- Jira: https://dbhlab.atlassian.net/browse/FOR-166

## Repo Notes (verified)

- `frontend/src/pages/MeasurementsPage.tsx` + `MeasurementForm`, `MetricCard`, `LineChart`/`ChartContainer`.
- Manual entry validation + manual/imported source indicator already present; keep them.

## Architectural Constraints

- Frontend-only, visual only; no changes to data or validation logic (validation stays server-authoritative
  where applicable, ADR-006).
- FOR-164 components + FOR-163 tokens; no hardcoded styling.
- Responsive + both themes + a11y (labels/errors) preserved.

## Common Pitfalls

- Moving validation errors away from their fields during restyle.
- Losing the manual/imported visual distinction (or making it color-only).
- Reintroducing hardcoded visuals in the page module.
- Breaking the empty-before-first-measurement state.

## Suggested Implementation Order

1. Latest-measurement cards + history list to the template via FOR-164 (+ tests).
2. Trend chart container framing (+ test).
3. Manual-entry form restyle keeping labels/validation adjacency (+ tests).
4. Responsive + theme + a11y pass.

## Validation

Run frontend checks. Confirm the view matches the template; data/validation/manual-imported distinction
and FOR-60 states preserved; responsive/both-themes/a11y hold; no hardcoded visuals.
