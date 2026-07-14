# FOR-111 AI Context

## Story

FOR-111 — ShoppingPage: category filter tabs + id-based product edit
(https://dbhlab.atlassian.net/browse/FOR-111)

## Intent

`ShoppingPage.tsx`'s own doc comment lists category grouping/filtering and
name-based product-edit resolution as documented gaps caused by the backend
not yet exposing `category`/`productId`. FOR-106 shipped both fields
(Done). This story closes the gap on the frontend side so the page matches
its mockup (`docs/5-lista-compra.png`) and stops the name-matching hack.

## Blocked by

None — relates to FOR-106 (Done). Not blocked by any of the FOR-107..110
backend prerequisites in this batch.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (read models, no duplicated domain logic)
- `specs/FOR-106/spec.md`, `specs/FOR-106/api.md` (the read-model fields
  this story consumes)
- `specs/FOR-61/` (accessible interaction patterns — tabs)
- Jira: https://dbhlab.atlassian.net/browse/FOR-111

## Domain Notes

- `frontend/src/pages/ShoppingPage.tsx` — read the full file-level doc
  comment first; it explicitly enumerates every gap this story and its
  siblings (FOR-113, FOR-117, FOR-118) close, and states the FOR-106
  precedent for the category/productId fields.
- `frontend/src/api/shopping.ts` — `ShoppingItem` type; needs `category`
  and `productId` fields if not already declared.
- Existing category tab markup (`ShoppingPage.tsx` ~L179–185) is already
  scaffolded with `role="tablist"`/`role="tab"` — extend it, don't
  reinvent the pattern.

## Architectural Constraints

- Frontend renders the read model as returned — no category inference or
  product matching logic beyond `productId` equality (ADR-006).
- Reuse `Card`, `Icon`, `Modal` as already used in `ShoppingPage.tsx`; no
  new shared components required for this story.

## Common Pitfalls

- Re-introducing name-based matching as a fallback "just in case" — the
  whole point of this story is to stop relying on `productName` for
  identity.
- Hiding items whose category doesn't match any known tab instead of
  falling back to "Otros"/"Todas".
- Touching `ProductEditModal`'s loading/error/not-found markup — that
  migration belongs to FOR-113, not this story.

## Suggested Implementation Order

1. Add `category`/`productId` to the `ShoppingItem` frontend type if
   missing.
2. Build category tabs from the list's distinct categories; wire filtering.
3. Change `ProductEditModal`'s lookup from `productName` match to
   `productId` match.
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise the Lista de compra screen with a multi-category
fixture and confirm tab filtering and id-based edit resolution.
