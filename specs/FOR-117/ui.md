# FOR-117 UI Spec

## Screens

- Lista de compra (`frontend/src/pages/ShoppingPage.tsx`) at
  `/lista-compra`. Mockup: `docs/5-lista-compra.png` — "PORCIONES" and
  "GENERADA" tiles this story restores.

## Components

- Item row (`ShoppingPage.tsx`'s `<li className={styles.item}>` block) —
  quantity display extended to include `unit`, plus an optional `servings`
  detail.
- `MetricCard` tiles in the budget/summary row (`styles.tiles`) — a new
  "Generada" tile (and, if the aggregate is meaningful, a "Porciones"
  tile) added alongside the existing "Productos"/"Total estimado"/
  "Estimado mensual" tiles.

## States

- Loading / Empty / Error: unchanged.
- Success: item rows and summary tiles show the enriched fields.

## Interactions

No new interactions — this story is display-only; FOR-118 adds the
interactive quantity-edit/regenerate/link-out controls.

## Accessibility

- New tile content follows the same `MetricCard` label/value/unit
  accessible structure already used by the existing tiles.
- Servings detail on an item row is programmatically associated with that
  item (not a separate unlabeled element).

## Responsive Behavior

- New tiles wrap into the existing `styles.tiles` responsive grid alongside
  the current three tiles, following the same mobile stacking behavior.
