# FOR-30 AI Context

## Story

FOR-30 — Create food item catalog
(https://dbhlab.atlassian.net/browse/FOR-30)

## Intent

Provide the nutrition building blocks: a `FoodItem` catalog that meal templates
(FOR-31) compose and macro calc (FOR-32) sums. Success is a constrained,
brand-free set of common foods, referenceable by stable id.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Nutrition → FoodItem; note the separate Shopping
  `MercadonaProduct`)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-003-persistence.md` (if seeded via migration)
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-24/` (ExerciseCatalog — the in-code catalog precedent to follow)
- Jira: https://dbhlab.atlassian.net/browse/FOR-30

## Domain Notes

- `FoodItem` = nutrition values per 100 g; **not** a purchasable product.
  Price/brand/store live in the Shopping context (FOR-5, `MercadonaProduct`),
  linked by id later — never mixed into `FoodItem`.
- Reference data, not user data — no ownership/scoping.

## Architectural Constraints

- `FoodItem` type in `.../domain/`, framework-free (ADR-001), JDBC + Flyway (no
  ORM) if persisted.
- Follow the FOR-24 `ExerciseCatalog` shape: a domain record + an in-code
  catalog with stable ids and lookup by id.

## Common Pitfalls

- Mixing product fields (price, url, brand) into `FoodItem`.
- Non-deterministic or clearly wrong seed values.
- Forgetting stable ids that FOR-31 meal items reference.

## Suggested Implementation Order

1. Define the `FoodItem` record with validation.
2. Decide persistence (in-code catalog vs. migration) and document it.
3. Seed the listed common foods with realistic per-100 g values + stable ids.
4. Add tests/validation for the seed and the food/product separation.

## Validation

Run `./gradlew test` from `backend/`.
