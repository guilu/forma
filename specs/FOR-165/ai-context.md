# FOR-165 AI Context

## Story

FOR-165 — Dashboard view: apply mockup template layout (`docs/1-dashboard.html`). Frontend-only, visual
refactor. Blocked by FOR-164.

## Intent

Make the dashboard look like the approved template using the refreshed components and tokens, without
touching data wiring or widget behaviour.

## Relevant Documents

- Template `docs/1-dashboard.html`; `specs/FOR-163/` (tokens), `specs/FOR-164/` (components).
- `specs/FOR-51/` (dashboard widgets), FOR-60 (states), FOR-62 (theme), FOR-61 (a11y).
- `AGENTS.md` — no domain logic in UI; tokens/components only.
- Jira: https://dbhlab.atlassian.net/browse/FOR-165

## Repo Notes (verified)

- `frontend/src/pages/DashboardPage.tsx` + `frontend/src/pages/dashboard/*Widget.tsx` + `WidgetSection`.
- Widgets already fetch data + render FOR-60 states + navigate; keep all of that.
- CSS Modules per page/widget — move visual rules into shared components/tokens where possible.

## Architectural Constraints

- Frontend-only; visual only. No changes to data fetching, read models, or navigation behaviour.
- Consume FOR-164 components + FOR-163 tokens; no hardcoded styling, minimise per-page overrides.
- Responsive + both themes + a11y preserved.

## Common Pitfalls

- Altering widget data/logic while restyling.
- Reintroducing hardcoded colors/spacing in `*.module.css` instead of using tokens/components.
- Dropping a FOR-60 state during the layout rework.
- Breaking the mobile single-column layout.

## Suggested Implementation Order

1. `DashboardPage` grid/sections to the template using `WidgetSection` + tokens (+ test).
2. Each widget card restyled via FOR-164 components (+ tests), states preserved.
3. Responsive + theme + a11y pass.

## Validation

Run frontend checks. Confirm the dashboard matches the template, all widgets keep data + FOR-60 states,
responsive/both-themes/a11y hold, and no hardcoded visuals remain.
