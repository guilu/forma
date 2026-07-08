# FOR-35 AI Context

## Story

FOR-35 — Create shopping product catalog model
(https://dbhlab.atlassian.net/browse/FOR-35)

## Intent

Give the Shopping context its core type: an editable purchasable product with
cost, optionally tied to a nutrition food. Success is a framework-free
`ShoppingProduct`, clearly separate from `FoodItem`, ready for the FOR-36 CRUD
API and FOR-38 budget calc.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Shopping → MercadonaProduct; Nutrition → FoodItem)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/` (domain-model precedent), `specs/FOR-30/` (FoodItem it may link to)
- Jira: https://dbhlab.atlassian.net/browse/FOR-35

## Domain Notes

- `ShoppingProduct` holds **purchase/cost** data; `FoodItem` (FOR-30) holds
  **nutrition** data. Never merge them; link softly by `linkedFoodItemId`.
- Prices are editable estimates — no external/automatic price fetching (MVP).

## Architectural Constraints

- Place the type under `.../domain/`, framework-free (ADR-001). Persistence
  (JDBC + Flyway) and the CRUD API are FOR-36, not this story.
- Use a currency-safe money type (`BigDecimal` recommended), consistent with
  FOR-36/FOR-38.

## Common Pitfalls

- Mixing nutrition values into `ShoppingProduct`.
- Using `double`/`float` for money (rounding) — prefer `BigDecimal`.
- Making the food link mandatory (it is optional).
- Implementing persistence here (that is FOR-36).

## Suggested Implementation Order

1. Decide money type and unit-price rule; document both.
2. Define the `ShoppingProduct` record with construction-time validation.
3. Unit-test creation, validation, and the optional food link.
4. Cross-check fields against docs/domain-model.md (note the naming mapping).

## Validation

Run backend unit tests (`./gradlew test` from `backend/`).
