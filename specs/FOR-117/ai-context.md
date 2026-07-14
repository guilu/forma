# FOR-117 AI Context

## Story

FOR-117 — Shopping list: richer item display (qty/unit/servings/generated-
at) (https://dbhlab.atlassian.net/browse/FOR-117)

## Intent

`ShoppingPage.tsx`'s doc comment explicitly documents this gap: "Quantity
unit... quantity is a plain integer with no unit field; shown as-is" and
"'PORCIONES' and 'GENERADA' budget tiles — the list/budget read models
carry no servings count or generation timestamp... so these tiles are
omitted rather than shown with invented data." FOR-108 closes the backend
side; this story closes the frontend side.

## Blocked by

FOR-108 (backend: exposes `unit`, `servings`, `generatedAt`). Do not start
implementation until FOR-108 has shipped a real API contract — this
story's fields do not exist yet.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (frontend renders read models, no derived
  domain math)
- `specs/FOR-108/spec.md`, `specs/FOR-108/api.md` (or `ui.md`'s API surface
  note) — the exact fields this story consumes
- Jira: https://dbhlab.atlassian.net/browse/FOR-117

## Domain Notes

- `frontend/src/pages/ShoppingPage.tsx` — read the file-level doc comment
  first; it explicitly lists the two gaps (quantity unit, PORCIONES/
  GENERADA tiles) this story closes, and cross-references the mockup.
- `frontend/src/api/shopping.ts` — `ShoppingItem`/`ShoppingList` types to
  extend with the new fields.
- `frontend/src/pages/progress/InsightsSection.tsx`'s `lastSyncFormatter`
  — existing `Intl.DateTimeFormat` pattern to reuse for `generatedAt`
  display, for consistency with how the app formats other timestamps.

## Architectural Constraints

- No unit conversion or serving-size math client-side — render exactly
  what the backend returns (ADR-006).
- Reuse `MetricCard` for any new summary tile; no new tile component.

## Common Pitfalls

- Fabricating a "Porciones" aggregate when the underlying data doesn't
  support a meaningful sum (mixed food/non-food items) — follow the
  documented decision in `spec.md` rather than guessing.
- Breaking existing item-row layout/styling while adding the unit/servings
  detail.

## Suggested Implementation Order

1. Confirm FOR-108 has shipped; extend `ShoppingItem`/`ShoppingList` types.
2. Update item-row rendering with unit (+ servings where present).
3. Add the "Generada" tile (and "Porciones" if the aggregate is
   meaningful).
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare the rendered Lista de compra screen against
`docs/5-lista-compra.png` for the restored tiles.
