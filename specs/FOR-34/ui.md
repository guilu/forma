# FOR-34 UI Spec

## Screens

- Nutrition page (`frontend/src/pages/NutritionPage.tsx`) — currently a
  `PagePlaceholder`; this story adds the running-day meal flow.

## Components

- Ordered meal flow: breakfast → lunch → pre-run snack → (run) → optional
  post-run recovery → light dinner.
- Meal item: meal name, preferred time, foods/quantities and (from FOR-32)
  macro totals.
- A short explanatory note ("carbs earlier, lighter dinner after a late run").
- Reuse `frontend/src/components/Card.tsx` and the header pattern used by the
  Dashboard/Training pages.

## States

- Loading: the flow area shows a loading indicator while fetching.
- Empty: no running-day template available → a clear message (not a broken
  layout).
- Error: data/API failure → a clear error state (ADR-006).
- Success: the ordered running-day flow with the explanation.

## Interactions

- Read-only in this slice: no meal editing.
- The optional post-run item is visually marked optional (e.g. an "opcional"
  label), not a required step.

## Accessibility

- Meals are an ordered list with text labels (order conveyed structurally, not
  by color alone).
- The explanation and optional marker are real text, announced to screen
  readers.
- Keyboard-reachable; visible focus via existing tokens.

## Responsive Behavior

- Mobile: meals stack vertically in order, readable, no horizontal scroll; the
  "carbs earlier / light dinner" intent stays obvious (docs/ui-guidelines.md
  mobile + "Late running nutrition UX").
- Desktop: may show the flow as a timeline/column using the same ordered data.
