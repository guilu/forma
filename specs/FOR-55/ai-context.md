# FOR-55 AI Context

## Story

FOR-55 — Create shopping assistant screens
(https://dbhlab.atlassian.net/browse/FOR-55)

## Intent

Turn the weekly nutrition plan into a realistic, checkable shopping list with
estimated cost and editable product references. Success is a category-grouped
list + budget summary + checked state + price/URL edit entry points.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (shopping checklist is a mobile priority),
  `docs/5-lista-compra.png` (mockup)
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-35/`/`FOR-36/` (product model/CRUD), `specs/FOR-37/`..`FOR-39/`
  (list/budget/checklist)
- Jira: https://dbhlab.atlassian.net/browse/FOR-55

## Domain Notes

- `frontend/src/pages/ShoppingPage.tsx`, `api/shopping.ts` exist (FOR-39, checklist
  + budget) — extend to categories/filters/edit entry points.
- Prices/names come from FOR-36 products; the list + check state from FOR-39; the
  budget from FOR-38. GOTCHA (FOR-39): UUID params bind differently H2 vs
  PostgreSQL — a backend concern, not UI.

## Architectural Constraints

- Consume shopping read models via `api/shopping.ts`; product edits via FOR-36.
  No pricing/budget math in the UI. Reuse FOR-50 list/budget primitives.

## Common Pitfalls

- Losing the list on a failed check toggle (must preserve).
- Showing list-regeneration/link-out as active without a backend.
- Broken layout on the empty-list state.

## Suggested Implementation Order

1. Category-grouped list + filters/sort from FOR-39.
2. Checked-item toggle (persist, preserve on failure).
3. Budget summary from FOR-38; product price/URL edit entry point (FOR-36).
4. Empty/loading/error states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/5-lista-compra.png`.
