# FOR-39 AI Context

## Story

FOR-39 — Create shopping list user interface
(https://dbhlab.atlassian.net/browse/FOR-39)

## Intent

Give the user a practical weekly checklist with cost, not just backend data.
Success is a mobile-friendly Shopping page showing the list, checkable items, and
weekly total + monthly estimate, backed by the API.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-005-api-design.md`
- `specs/FOR-37/` (list), `specs/FOR-38/` (budget), `specs/FOR-36/` (products)
- `specs/FOR-18/`, `specs/FOR-26/`, `specs/FOR-27/` (frontend patterns:
  apiClient, states, Card, check-toggle, and the "no API yet" data-source gap)
- Jira: https://dbhlab.atlassian.net/browse/FOR-39

## Domain Notes

- The frontend renders read models (list items, costs, budget) and toggles
  checked state; it owns no cost/budget rules (ADR-006).
- Editing product prices is a separate story — not here.

## Architectural Constraints

- Replace `frontend/src/pages/ShoppingPage.tsx` (currently a `PagePlaceholder`);
  routing at `/lista-compra` is already wired.
- Call the API only through `frontend/src/api/client.ts` (relative `/api/...`).
- Reuse `Card`, design tokens; handle loading/empty/error states. Currency
  formatting in EUR.

## Common Pitfalls

- Assuming a list/budget/check API exists — FOR-36 covers products only. Resolve
  the data source explicitly (spec.md Open Questions).
- Duplicating cost/budget math client-side.
- Losing the list on a failed check toggle.
- Skipping mobile layout or the empty/error states.

## Suggested Implementation Order

1. Resolve the list/budget read + check-toggle data source (endpoints or interim)
   and document it.
2. Build the checklist (items, quantity, cost, checkbox) with `Card`.
3. Show weekly total + monthly estimate with EUR formatting.
4. Wire check/uncheck via `apiClient`; add loading/empty/error states; verify
   mobile.

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`); backend tests if endpoints are added.
