# FOR-55 UI Spec

## Screens

- Lista de compra (`frontend/src/pages/ShoppingPage.tsx`) at `/lista-compra`.
  Mockup: `docs/5-lista-compra.png`.

## Components

- Budget summary cards: PRODUCTOS, TOTAL ESTIMADO, PORCIONES, GENERADA.
- Category filter tabs (Todas / Frutas y verduras / Proteínas / …) + sort.
- Category-grouped list; per-item row: name, quantity (+/- if editable), unit,
  price, actions (check, edit price/URL), checked state.
- Reuse FOR-50 list + budget-summary primitives; existing `Card`.

## States

- Loading: list/budget skeletons (FOR-60).
- Empty: no items this week → clear message, total `0,00 €`.
- Error: load failure → error + retry; toggle failure → inline error, list kept.
- Success: grouped list + weekly/monthly budget.

## Interactions

- Check/uncheck an item → persists via `api/shopping`; row updates.
- Filter/sort → reflow the list.
- Edit price/URL → product edit (FOR-36) entry point.
- List regeneration / product link-out only if backed; otherwise not active.

## Accessibility

- Each item is a labelled checkbox (name/quantity/price associated).
- Totals are text, announced; empty/error announced.
- Keyboard-operable checkboxes/filters with visible focus.

## Responsive Behavior

- Desktop: full table with actions column.
- Mobile: grouped cards, "Ver más (N)" per category, touch-friendly checkboxes,
  sticky totals; no horizontal scroll (shopping checklist is a mobile priority).
