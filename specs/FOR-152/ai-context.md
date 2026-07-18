# FOR-152 AI Context

## Story

FOR-152 — Catálogos reales: comida + Mercadona + umbral de coste. Slice 4 of epic FOR-148. Seeds
plan (fijo) catalogs and adds the < 120 €/week cost threshold. Enables FOR-150 rule 6.

## Intent

Swap generic demo catalogs for Diego's real 23 foods + 23 Mercadona products and add the cost
threshold. Success = catalogs match the Macros/Mercadona sheets, weekly total ≈ 104.11 €, and the
dashboard + insights show an over/under-120 € signal.

## Relevant Documents

- `AGENTS.md` — hexagonal; no fabricated values; seed data allowed as fixtures/reference data.
- `docs/fitness_os.xlsm` — sheets **Macros** (23 foods), **Mercadona** (23 products + links/prices), **Compra** (weekly total 104.109 €), **Dashboard** (< 120 €/sem target). Source of truth.
- `docs/adr/ADR-001-architecture.md` (cost logic in domain, not UI), `ADR-002-authentication.md` (owner-scoping), `ADR-003-persistence.md` (additive migrations, one column per statement), `ADR-006-frontend.md` (UI renders backend read models).
- Sibling: `specs/FOR-150/` (rule 6 consumes the threshold).
- Jira: https://dbhlab.atlassian.net/browse/FOR-152

## Domain / Repo Notes (verified)

- `FoodCatalog` — 12 in-code foods (FOR-30), no migration; `FoodItem` has FOR-134 key-nutrient fields (leave null when unknown).
- `shopping_products` table (V4) already has `url TEXT`; `category` added by V7 (`NOT NULL DEFAULT 'OTROS'`). No schema change needed to store links/categories — data only.
- `V5__shopping_lists.sql` seeds 4 demo products + one weekly list (~25 €); replace these.
- `ShoppingBudgetCalculator` — weekly = Σ(price × qty), monthly = weekly × 4.33; missing price → 0. No threshold today.
- `ShoppingProduct` record — `url`, `packageSize`, `category`, `linkedFoodItemId` supported.
- `frontend/.../dashboard/ShoppingWidget.tsx` — weekly + monthly tiles from `getShoppingList()`; add a threshold signal (see `ui.md`).
- Migration head **V19**. **H2 lesson (V7 comment): one `ADD COLUMN` per ALTER statement** — H2 rejects multi-column ALTER.

## Architectural Constraints

- Additive migration for the shopping data (next free `V<N>` above V19); one column per statement if altering; never edit V4/V5/V7.
- Money as `BigDecimal`/`NUMERIC`. Weekly total stays derived (don't persist it).
- Threshold is a single plan constant/config; cost comparison lives in the domain (ADR-001), not the widget.
- Owner-scoped (ADR-002). UI renders the backend signal (ADR-006).

## Common Pitfalls

- Leaving the 4 demo products/list from V5 alongside the 23 real ones (double seed).
- Fabricating key nutrients for the 23 foods — the Macros sheet has none; keep them null.
- Multi-column ALTER on H2 (use one statement per column).
- Recomputing the budget or the threshold in the frontend (ADR-001/ADR-006).
- Hard-coding the migration version instead of claiming the next free `V<N>`.
- Storing the weekly total as a column instead of deriving it.

## Suggested Implementation Order

1. Reseed `FoodCatalog` to the 23 Macros foods (macros + ración; key nutrients null; stable ids).
2. Additive migration: 23 Mercadona products (url/price/category/package_size/qty) + rebuilt weekly list (total ≈ 104.11 €), replacing the demo seed.
3. Add the < 120 €/week threshold to the domain + budget read model (over/under signal).
4. Surface the signal in `ShoppingWidget` (see `ui.md`); expose it for FOR-150 rule 6.

## Validation

Backend build + tests (`./gradlew build`) + frontend tests. Confirm: 23 foods match Macros; 23 products match Mercadona with links/prices; derived weekly total ≈ 104.11 €, monthly ≈ 450.79 €; threshold signal correct around 120 €; migration additive above V19 with one column per statement; no demo rows remain; key nutrients not fabricated; widget renders the backend signal.
