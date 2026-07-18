# FOR-152 Test Plan

Strict TDD: failing tests first (food catalog → shopping seed/migration → threshold → widget), then implement.

## Scope

The 23-food catalog reseed, the 23-product Mercadona migration + rebuilt weekly list, the < 120 €
threshold in the budget read model, and the dashboard signal.

## Domain Tests

- `FoodCatalog` contains the 23 Macros foods with exact kcal/prot/HC/grasa + ración; key nutrients null where the Excel gives none (no fabrication).
- `ShoppingBudgetCalculator` weekly total over the seeded list ≈ 104.11 €; monthly ≈ 450.79 € (× 4.33).
- Threshold logic: over/under 120 € computed correctly; boundary at exactly 120 € documented (Excel ">120").

## Application / Persistence Tests

- Migration seeds exactly 23 `shopping_products` with url/price/category/package_size; the demo 4 products/list are gone.
- Rebuilt weekly `shopping_lists` + items derive to ≈ 104.11 €.
- Migration is additive above V19; any ALTER uses one column per statement.

## API Tests

- `GET /api/v1/nutrition/foods` returns 23 foods (Macros values).
- `GET /api/v1/shopping/products` returns 23 products with real links/prices.
- `GET /api/v1/shopping/list` budget includes `weeklyThresholdEur` + `overThreshold`.

## UI Tests

- `ShoppingWidget` renders the OK signal when under 120 € and the over-budget signal when ≥ 120 €, from the backend field (no client calc).
- State conveyed by text/label, not color alone.

## Edge Cases

- Weekly total just under/over 120 €.
- Prorated weekly quantities (e.g. aceite 0.15 botella) preserved.
- A product missing a price contributes zero (should not occur in the 23-product seed).

## Fixtures

- The 23 Macros foods and 23 Mercadona products (name/link/price/qty/category) as seed fixtures.
- H2-in-PostgreSQL-mode with Flyway running all migrations including the new `V<N>`.
- Frontend fixture budgets under and over 120 € for the widget.
