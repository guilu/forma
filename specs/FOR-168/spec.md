# FOR-168 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-168
Epic: FOR-162 Design System v2. Blocked by FOR-164 (shared components).

## Summary

Refactor the nutrición / nutrition view to match `docs/4-nutricion.html`, using the reconciled tokens
(FOR-163) and refreshed shared components (FOR-164). Visual only — preserve data, day-type guidance and
states. Frontend-only.

## Repository baseline (verified)

- `frontend/src/pages/NutritionPage.tsx` (+ `.module.css`) — day-type selector (running/strength/rest),
  daily meal plan, meal detail cards, macro summary (`MacroRing`), FOR-60 states.
- Template: `docs/4-nutricion.html`.

## Functional Requirements

- Align the day-type selector, daily meal plan, meal detail cards and macro summary with the template, via
  FOR-164 components + FOR-163 tokens.
- Remove per-page visual overrides now covered by shared components/tokens.
- Preserve data wiring, day-type guidance (e.g. running-day / light-dinner notes) and FOR-60 states.

## Non-Functional Requirements

- Responsive + both themes (FOR-62); a11y preserved (FOR-61).
- Token/component-driven styling only; no nutrition calculations in the UI (ADR-001).

## UI / States (see ui.md)

- Selector, meal plan, meal cards, macro summary restyled; states preserved.

## Edge Cases

- Empty/error meal plan → template-consistent FOR-60 states.
- Macro summary (`MacroRing`) values come from the backend — visual restyle only, no recompute.
- Day-type guidance remains legible per selected day.

## Open Questions

- If the template restructures the meal plan (e.g. timeline vs cards), follow it and document.
- Any new macro/meal card variant → raise in FOR-164.
