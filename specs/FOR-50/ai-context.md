# FOR-50 AI Context

## Story

FOR-50 — Define MVP design system
(https://dbhlab.atlassian.net/browse/FOR-50)

## Intent

Keep the product visually coherent and cut duplicated UI work with a small token
set + reusable components. Success is a single source of tokens and reusable
button/card/input/badge/chart-container primitives that every screen composes.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (palette, typography, restrained accent, "not a
  Christmas tree")
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-007-testing.md`
- Mockups for component inventory: `docs/1-dashboard.png`..`docs/8-configuracion.png`
- Jira: https://dbhlab.atlassian.net/browse/FOR-50

## Domain Notes

- Tokens already exist in `frontend/src/styles/theme.css` (FOR-81): surfaces,
  text, accent `#9dff57`, warning, danger, spacing, radius, typography, layout,
  elevation — dark default + `[data-theme='light']` override. Do NOT redefine;
  extend only if a component needs a missing token.
- `components/Card.tsx`, `MetricCard.tsx`, `LineChart.tsx`, `Modal.tsx`,
  `Icon.tsx`, `Brand.tsx` already exist — build on them.

## Architectural Constraints

- Base components under `frontend/src/components/`, token-driven, no feature
  logic (ADR-006). No hardcoded visual rules in feature pages.

## Common Pitfalls

- Reinventing tokens that already exist in `theme.css`.
- Hardcoding colors/spacing inside components instead of using CSS variables.
- Over-engineering (heavy component library) for an MVP.

## Suggested Implementation Order

1. Audit tokens in `theme.css`; add only gaps components require.
2. Implement Button + Badge/Status variants; align inputs with `MeasurementForm`.
3. Add a chart-container wrapper around `LineChart`.
4. Provide usage examples; unit-test variants render with correct token classes.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Visually verify variants against the mockups in both themes.
