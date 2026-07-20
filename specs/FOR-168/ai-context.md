# FOR-168 AI Context

## Story

FOR-168 — Nutrición view: apply mockup template layout (`docs/4-nutricion.html`). Frontend-only visual
refactor. Blocked by FOR-164.

## Intent

Restyle the nutrition view to the approved template using refreshed components/tokens, preserving data,
macro summary and day-type guidance.

## Relevant Documents

- Template `docs/4-nutricion.html`; `specs/FOR-163/`, `specs/FOR-164/`.
- `specs/FOR-54/` (nutrition screens), FOR-60 (states), FOR-61 (a11y), FOR-62 (theme).
- `AGENTS.md` — no nutrition calculations in UI (ADR-001).
- Jira: https://dbhlab.atlassian.net/browse/FOR-168

## Repo Notes (verified)

- `frontend/src/pages/NutritionPage.tsx` + `MacroRing` (macro summary). Day-type selector + meal cards present.
- Macro values are backend-computed; the UI renders them (no recompute).

## Architectural Constraints

- Frontend-only, visual only; no macro/nutrition math in UI (ADR-001); no change to data.
- FOR-164 components + FOR-163 tokens; no hardcoded styling.
- Responsive + both themes + a11y preserved.

## Common Pitfalls

- Recomputing macros in the UI while restyling `MacroRing`.
- Losing day-type guidance (running-day / light-dinner notes).
- Hardcoding visuals instead of tokens/components.
- Dropping a FOR-60 state.

## Suggested Implementation Order

1. Day-type selector + daily meal plan layout to the template via FOR-164 (+ tests).
2. Meal detail cards + macro summary (`MacroRing`) restyle (+ tests), values preserved.
3. Responsive + theme + a11y pass.

## Validation

Run frontend checks. Confirm the view matches the template; data, macro summary and day-type guidance and
FOR-60 states preserved; responsive/both-themes/a11y hold; no hardcoded visuals; no macro math in UI.
