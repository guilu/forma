# FOR-35: Create shopping product catalog model

Jira: https://dbhlab.atlassian.net/browse/FOR-35
Epic: FOR-5 Shopping Assistant

## Summary

Create the `ShoppingProduct` domain model: an editable, purchasable product with
cost information, optionally linked to a FOR-30 `FoodItem`. This is the purchase/
cost side of nutrition — kept **separate** from `FoodItem` (which holds nutrition
values). Domain-only in this story; persistence + API arrive in FOR-36.

## User/System Flow

This story has no user-facing flow. It defines the type consumed by later
stories:

1. FOR-36 persists and exposes products through a CRUD API.
2. FOR-37 shopping-list items reference products by id.
3. FOR-38 sums product costs into weekly/monthly budgets.

## Functional Requirements

- Add `ShoppingProduct` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (no Spring/JPA/JDBC/HTTP — ADR-001), following the FOR-15 precedent.
- Fields (docs/domain-model.md "MercadonaProduct"): `name`, product `url`,
  `packageSize`, `estimatedPriceEur`, `pricePerUnitEur`, optional
  `linkedFoodItemId` (a FOR-30 food id), `lastCheckedAt`, `notes`.
- The product can **optionally** link to a `FoodItem` (nullable
  `linkedFoodItemId`); it must work fine without a link.
- Unit price can be stored or derived from price / package size — document the
  chosen rule.
- Keep `ShoppingProduct` free of nutrition values (those stay in `FoodItem`).

## Non-Functional Requirements

- Prices are **editable estimates** in the MVP; no external price sync.
- Deterministic value type; no persistence introduced here (FOR-36 adds it).

## Data Model Notes

Mirrors docs/domain-model.md's `MercadonaProduct`. **Naming discrepancy**: the
Jira story calls it `ShoppingProduct` while docs/domain-model.md calls it
`MercadonaProduct`; use the story name `ShoppingProduct` (the model is not
strictly Mercadona-specific) and note the mapping. `linkedFoodItemId` is a soft
reference to the FOR-30 food catalog (Shopping ↔ Nutrition link), not an embedded
food.

## Edge Cases

- Negative or zero price / package size — validate at construction.
- Missing/blank `url` — decide whether required (recommend optional).
- `linkedFoodItemId` present but not resolvable — referential integrity is a
  concern for the API/consumer, not necessarily this type; document.

## Open Questions

- Store `pricePerUnitEur` vs. derive from `estimatedPriceEur` / `packageSize`.
  Recommend deriving if `packageSize` is numeric; if `packageSize` is a free-text
  label (e.g. "1 kg"), store the unit price. Document the choice.
- Money representation: `BigDecimal` (recommended for currency) vs. a minor-unit
  integer — pick one and keep it consistent across FOR-35/36/38.
