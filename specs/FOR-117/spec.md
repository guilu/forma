# FOR-117: Shopping list: richer item display (qty/unit/servings/generated-at)

Jira: https://dbhlab.atlassian.net/browse/FOR-117
Epic: FOR-47 UI & UX

## Summary

`ShoppingPage.tsx`'s own doc comment documents that quantity is shown as a
bare integer with no unit, and that the "PORCIONES" and "GENERADA" budget
tiles from the mockup (`docs/5-lista-compra.png`) are omitted because "the
list/budget read models carry no servings count or generation timestamp."
FOR-108 (backend) adds `unit`, `servings` per item and `generatedAt` on the
list. This story is the frontend consumption: render the richer item line
and restore the two omitted mockup tiles.

## User/System Flow

1. User opens Lista de compra (`/lista-compra`).
2. Each item line now shows quantity with its unit (e.g. "14 ud") and, when
   applicable, a servings indicator (e.g. "2 raciones").
3. The page shows a "Generada" tile/label with the list's `generatedAt`
   timestamp, matching the mockup.

## Functional Requirements

- Read `unit`, `servings` (nullable) per item and `generatedAt` on the list
  from `GET /api/v1/shopping/list` (FOR-108).
- **Item line**: render `quantity` + `unit` together (e.g. "14 ud", "500 g")
  instead of the current bare number (`ShoppingPage.tsx` ~L205); when
  `servings` is present, show it as a secondary detail on the same row.
- **"Generada" tile/label**: restore the mockup's generation-timestamp
  display, formatted consistently with other date displays in the app
  (`Intl.DateTimeFormat`, same pattern as `InsightsSection.tsx`'s
  `lastSyncFormatter`).
- **"Porciones" tile**: if a meaningful aggregate servings count exists
  across the list (sum of per-item `servings` where present), restore this
  tile; if the aggregate is not meaningful (e.g. mixed food/non-food
  items), keep it item-level only and document why the aggregate tile is
  omitted rather than fabricating a number.
- No unit conversion or serving-size math in the UI — render exactly what
  the backend returns (ADR-006).

## Non-Functional Requirements

- No regression to existing checklist/budget rendering or interactions.
- Token-driven styling consistent with the existing item-row markup.

## Data Model Notes

`ShoppingItem`/`ShoppingList` frontend types (`frontend/src/api/
shopping.ts`) need `unit`, `servings` and `generatedAt` fields added,
mirroring FOR-108's API response shape.

## Edge Cases

- Item with `servings: null` (non-food item) → no servings detail shown,
  quantity+unit only.
- Pre-migration list without a real `generatedAt` (backfilled value per
  FOR-108) → still renders a date, not a blank/broken tile.
- Unit value the frontend doesn't have a specific display label for →
  render the raw enum/string value rather than crashing or hiding it.

## Open Questions

- Whether the "Porciones" tile shows a per-list aggregate or is dropped in
  favor of per-item display only — depends on how meaningful the aggregate
  is once FOR-108 ships; decide during implementation and document.
