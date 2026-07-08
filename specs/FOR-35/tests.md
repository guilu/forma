# FOR-35 Test Plan

## Scope

Unit-test the `ShoppingProduct` domain type: creation, validation, and the
optional food link. No persistence, API or UI in this story.

## Domain Tests

- A valid product is created with all fields set correctly.
- A product with no `linkedFoodItemId` (unlinked) is valid.
- A product linked to a food id stores it correctly.
- Construction-time validation rejects invalid values (e.g. negative price or
  package size), per spec.md.
- Unit price is stored or derived per the documented rule.

## Application Tests

N/A — no application/use-case layer is introduced by this story.

## API Tests

N/A — the CRUD API is FOR-36.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Negative/zero price or package size.
- Missing optional fields (`url`, `notes`, `lastCheckedAt`).
- Money rounding (assert the chosen `BigDecimal` scale/behaviour).

## Fixtures

- A "linked" product (with `linkedFoodItemId`).
- An "unlinked" product (no food link).
