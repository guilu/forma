# FOR-55: Create shopping assistant screens

Jira: https://dbhlab.atlassian.net/browse/FOR-55
Epic: FOR-47 UI & UX

## Summary

Build the shopping (Lista de compra) screens: weekly list grouped by category,
quantities, estimated weekly + monthly cost, product price/URL edit entry points,
checked-item state and a budget summary. Mockup: `docs/5-lista-compra.png`.
`ShoppingPage.tsx` already renders the checklist + budget (FOR-39); this story
aligns it to the richer mockup (categories, filters, edit entry points).

## User/System Flow

1. User opens Lista de compra (`/lista-compra`).
2. The weekly list + budget load from the Shopping read models (FOR-39 list,
   FOR-38 budget), with product names/prices from FOR-36.
3. User checks/unchecks items (FOR-39), and can surface price/URL editing for a
   product (FOR-36).

## Functional Requirements

- **Weekly list view** grouped by category (Frutas y verduras, Proteínas,
  Lácteos y huevos, …) with category filter tabs + sort.
- **Per-item row**: product name, quantity (+ unit), estimated price, checked
  state; quantity/price/URL edit entry points where backed.
- **Budget summary**: PRODUCTOS count, TOTAL ESTIMADO (weekly), monthly estimate,
  PORCIONES, generated timestamp (FOR-38 budget + FOR-39 list).
- **Checked item state**: toggle persists via FOR-39; row reflects it.
- **Product price/URL edit**: entry point to edit a product (FOR-36 CRUD) — MVP
  treats URLs/prices as user-managed estimates.
- Empty (no list), loading and error states (FOR-60).

## Non-Functional Requirements

- EUR currency formatting; no fake precision.
- Consumes read models; toggle preserves the list on failure (FOR-39 pattern).

## Data Model Notes

Consumes FOR-39 weekly list + check toggle, FOR-38 budget, FOR-36 product CRUD
(name/price/URL). **Mockup extras not yet backed**: "Generar nueva lista",
per-item Mercadona link-out + add-to-cart icons, "porciones para 7 días" — render
only what an API supports; otherwise placeholder/omit (repository priority).

## Edge Cases

- Empty list this week → clear empty state; total `0,00 €`, not broken.
- Check toggle failure → item state reverts / error shown, list preserved.
- Unknown category → grouped under "Otros".

## Open Questions

- List regeneration and product link-out/add-to-cart exceed current backend —
  recommend surfacing check + budget + price/URL edit for the MVP and documenting
  the rest.
- Whether quantity is editable in the MVP (list is generated) — document.
