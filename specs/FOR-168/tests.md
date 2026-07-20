# FOR-168 Test Plan

Strict TDD where practical. Visual refactor — preserve behaviour; re-baseline snapshots intentionally.

## Scope

`NutritionPage` presentation (day-type selector, meal plan, meal cards, macro summary). Data unchanged, out of scope.

## Behaviour (must not regress)

- Day-type selector switches the plan; guidance text renders per day type.
- Meal plan + meal detail cards render from data.
- `MacroRing` renders backend macro values (no recompute).
- FOR-60 loading/empty/error states preserved.

## Visual / layout

- Page matches the template; snapshots re-baselined (diff reviewed).
- FOR-164 components + tokens (no hardcoded hex where practical).
- Responsive single-column on mobile.

## Accessibility

- Selector is keyboard-operable with labels; macro summary has text equivalents (not color alone);
  contrast both themes; axe passes.

## Fixtures

- Existing nutrition mocks (plan per day type, macro summary, empty/error), reused.
