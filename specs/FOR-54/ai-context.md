# FOR-54 AI Context

## Story

FOR-54 — Create nutrition planner screens
(https://dbhlab.atlassian.net/browse/FOR-54)

## Intent

Let users follow the nutrition plan without recomputing meals/macros/day types.
Success is a day-type-aware meal plan + macro summary + running-day guidance,
reading only existing nutrition read models.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (late-running nutrition UX, calm copy),
  `docs/4-nutricion.png` (mockup)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-29/`..`FOR-34/` (nutrition domain, templates, running-day flow)
- Jira: https://dbhlab.atlassian.net/browse/FOR-54

## Domain Notes

- `frontend/src/pages/NutritionPage.tsx`, `api/nutrition.ts` exist (FOR-33/34) —
  extend to the mockup.
- Macros/targets come from FOR-32; day templates from FOR-33; the running-day
  flow from FOR-34. UI never recomputes macros.
- Meal logging, hydration and key-nutrient panels are not backed yet.

## Architectural Constraints

- Consume nutrition read models via `api/nutrition.ts`. No calculations in the
  UI. Reuse FOR-50 macro-summary/card primitives.

## Common Pitfalls

- Doing macro math in the UI.
- Showing meal-logging/hydration as active when the backend can't persist them.
- Losing the carbs-early / lighter-dinner emphasis on running days.

## Suggested Implementation Order

1. Day-type selector + daily meal list from FOR-33.
2. Macro summary (vs target) from FOR-32.
3. Running-day guidance from FOR-34; recovery recommendation when present.
4. Shopping shortcut; empty/error states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/4-nutricion.png`.
