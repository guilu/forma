# FOR-39: Create shopping list user interface

Jira: https://dbhlab.atlassian.net/browse/FOR-39
Epic: FOR-5 Shopping Assistant

## Summary

Replace the placeholder `ShoppingPage`
(`frontend/src/pages/ShoppingPage.tsx`) with a mobile-friendly weekly shopping
checklist: items with quantity and estimated cost, checkable state, and the
weekly total + monthly estimate (FOR-38). Reads through the shared API client.

## User/System Flow

1. User opens the Shopping page (routed at `/lista-compra`, currently a
   `PagePlaceholder`).
2. The frontend loads the weekly list (FOR-37) with item costs and the budget
   (FOR-38), and renders a checklist plus the weekly total and monthly estimate.
3. The user checks/unchecks items; the checked state is persisted (via the API).

## Functional Requirements

- Shopping page shows the weekly list: per item — name, quantity, estimated
  cost, and checked state.
- User can **check and uncheck** items (persisted through the API).
- Weekly total is visible; monthly estimate is visible — both with currency
  formatting (EUR).
- Call the backend only through the shared `apiClient` (relative `/api/...`,
  ADR-006); reuse existing UI primitives (`Card`, tokens).
- Loading, empty and error states (ADR-006).

## Non-Functional Requirements

- Mobile-friendly checklist (docs/ui-guidelines.md); large touch targets, no
  horizontal scroll.
- No domain logic duplicated in the frontend — costs/budget come from the API.

## Data Model Notes

Consumes the FOR-37 weekly list + FOR-38 budget. **Repo gap**: no Shopping
read/check API for the *list* exists yet (FOR-36 is products only). Serving the
weekly list, its budget, and persisting checked state needs endpoints — resolve
during implementation (see Open Questions); do not invent an unspecified
contract.

## Edge Cases

- Empty week (no items) — empty state with the total shown as 0, not a broken
  layout.
- Toggling a check while offline / on API failure — show an error, don't lose
  the list (ADR-006).
- Long product names / large quantities do not break the mobile layout.

## Open Questions

- **Data + check API**: which endpoints feed the list, budget, and persist
  checked state? FOR-36 covers products, not the list. Options: add list/budget
  read endpoints + a check-toggle endpoint (like the FOR-27 status pattern), or
  an interim source. Document the chosen approach; keep the frontend on
  `apiClient` relative paths.
- Editing product prices from this page is explicitly deferred to a separate
  story (Jira technical notes) — out of scope here.
