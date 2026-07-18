# FOR-152 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-152
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheets **Macros**, **Mercadona**, **Compra**, **Dashboard** (cost target).
Slice 4 of 7. Enables slice 2 rule 6 (cost > 120 €) in FOR-150.

## Summary

Replace the demo catalogs with Diego's real data: 23 foods (Macros), 23 Mercadona products
(Mercadona/Compra) with links/prices/quantities, and a **< 120 €/week** cost threshold surfaced on
the dashboard and to insights (rule 6). Plan (fijo) reference data.

## Excel data (verified)

- **Macros — 23 foods** (kcal/prot/HC/grasa per 100 g + ración g). Examples: Copos de avena 370/13/60/7 (60 g) · Whey proteína 390/78/8/6 (30 g) · Pechuga pollo 110/23/0/2 (200 g) · Merluza 74/16/0/1 (200 g) · Salmón 208/20/0/13 (180 g) · Huevos 143/13/1/10 · Atún natural 116/25/0/1 · Aceite oliva 900/0/0/100. Full list of 23 in the Macros sheet.
- **Mercadona — 23 products**: producto, categoría, formato, cantidad semanal, unidad, precio €, coste semanal €, link Mercadona. Examples: Pechugas enteras de pollo 2 kg × 7.20 = 14.40 · Salmón 0.7 kg × 14.50 = 10.15 (principal variable de presupuesto) · Merluza 1 kg × 7.50 · Copos de avena Brüggen 1.55. Full list of 23 in the Mercadona sheet, each with a `tienda.mercadona.es` link (prices "estimado editable").
- **Compra — weekly total 104.109 €/week**; monthly ×4.33 = 450.79 €.
- **Cost target — < 120 €/week** (Dashboard: "Coste compra semanal 104.109, objetivo <120 €/sem, OK").

## Current repository state (verified)

- `domain/FoodCatalog.java` — **12** in-code foods with generic macros (different from Diego's). No food persistence/migration today (in-code reference data, FOR-30).
- `resources/db/migration/V5__shopping_lists.sql` — seeds only **4** generic `shopping_products` (Avena/Pollo/Arroz/Plátano, no `url`) + one weekly list (~25 €/week).
- `resources/db/migration/V4__shopping_products.sql` — table already has a `url TEXT` column; `V7` added `category NOT NULL DEFAULT 'OTROS'`. So no schema change is needed to store links/categories — only data.
- `domain/ShoppingBudgetCalculator.java` — weekly = Σ(unit price × qty), monthly = weekly × 4.33 (correct), but **no < 120 € threshold** concept.
- `frontend/src/pages/dashboard/ShoppingWidget.tsx` — shows weekly + monthly tiles; **no threshold signal**.
- Migration head **V19**.

## Functional Requirements

- Reseed the food catalog to Diego's **23 Macros foods** with his macros + ración. Keep the current in-code `FoodCatalog` approach (no migration) unless food persistence is introduced; leave FOR-134 key-nutrient fields null where the Excel gives no confident value (never fabricate).
- Reseed the shopping catalog to the **23 Mercadona products** via an additive migration (next free `V<N>` at implementation time — do NOT hard-code; head is V19; one column per statement if any ALTER is needed, ADR-003), each with real name, `url`, price, `package_size`, `category`, and weekly quantity; rebuild the weekly `shopping_lists` / items so the derived weekly total ≈ **104.11 €**. Replace the 4 demo products/list from V5 (do not leave demo rows).
- Add a **< 120 €/week** cost threshold: a domain concept (constant/config) exposed in the shopping budget read model as an over/under signal, consumed by the dashboard `ShoppingWidget` and by FOR-150 rule 6.

## Non-Functional Requirements

- Additive migration only (ADR-003) — earlier migrations untouched; one column per statement if altering.
- Money as `BigDecimal`/`NUMERIC` (currency-safe, existing convention). Weekly total derived, not stored raw.
- Owner-scoped (ADR-002); explainable — all data traceable to the Macros/Mercadona/Compra sheets.
- Prices are editable estimates (existing MVP rule); links are reference only, no external price sync.

## Data Model Notes

- `FoodItem` already carries key-nutrient fields (FOR-134); the Macros sheet only provides kcal/prot/HC/grasa + ración, so key nutrients stay null unless a confident reference exists.
- `ShoppingProduct`/`shopping_products` already support `url`, `package_size`, `category`, `linked_food_item_id` — reuse the soft link to the food ids where names line up.
- The 120 € threshold is a single plan constant; decide domain constant vs profile field and document. Weekly cost stays **derived** by `ShoppingBudgetCalculator` (don't persist the total).

## Edge Cases

- Weekly total just under/over 120 € → correct signal (document inclusive/exclusive per Excel ">120").
- A product with no price → contributes zero (existing rule); ensure all 23 have prices so the ≈104.11 € total holds.
- Prorated items (e.g. aceite 0.15 botella/week) preserved from the Mercadona sheet.

## Open Questions

- Food catalog: stay in-code (reseed `FoodCatalog`) vs move to a seeded table — recommend in-code (matches FOR-30) unless persistence is otherwise needed.
- Threshold location: domain constant vs a profile/plan config field (ties to FOR-149).
- Whether to link each Mercadona product to a food id (soft link) for all 23 or only where names match cleanly.
