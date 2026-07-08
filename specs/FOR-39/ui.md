# FOR-39 UI Spec

## Screens

- Shopping page (`frontend/src/pages/ShoppingPage.tsx`) — currently a
  `PagePlaceholder`; this story adds the weekly checklist + budget.

## Components

- Weekly checklist: one row per item with a checkbox, product name, quantity and
  estimated cost.
- Budget summary: weekly total and monthly estimate, with EUR currency
  formatting.
- Reuse `frontend/src/components/Card.tsx` and the header pattern used by the
  Dashboard/Training/Nutrition pages.

## States

- Loading: the list/budget area shows a loading indicator while fetching.
- Empty: no items this week → a clear message, total shown as `0,00 €`, not a
  broken layout.
- Error: list/budget load failure, or a failed check toggle → a clear error
  state (ADR-006); the list is preserved on a toggle failure.
- Success: the checklist plus weekly total and monthly estimate.

## Interactions

- Checking/unchecking an item persists via `apiClient` and updates the row's
  state.
- Read-only otherwise: editing product prices is a separate story.

## Accessibility

- Each item is a labelled checkbox (name/quantity/cost associated with it).
- Totals are text, announced to screen readers; empty/error states announced.
- Keyboard-operable checkboxes with visible focus (existing tokens).

## Responsive Behavior

- Mobile: the checklist stacks; rows and checkboxes are touch-friendly; totals
  stay visible (sticky or at the top/bottom); no horizontal scroll
  (docs/ui-guidelines.md mobile priority: shopping checklist).
- Desktop: same list/budget, comfortably spaced.
