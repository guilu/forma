# FOR-30 Test Plan

## Scope

Verify the `FoodItem` model and the initial catalog: validation, seed coverage,
stable ids, and the food/product separation.

## Domain Tests

- A valid `FoodItem` is created with per-100 g values and a default serving.
- Construction rejects invalid values (negative macros, non-positive serving).
- `FoodItem` carries no product fields (price/brand/url are absent by design).

## Application Tests

- The initial catalog includes the listed common foods (oats, eggs, yogurt,
  fresh cheese, chicken, turkey, fish, rice, potatoes, banana, vegetables, whey
  protein).
- Ids are unique; lookup by id works and returns empty for unknown ids.
- If persisted, seeding is deterministic and idempotent.

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Duplicate food ids/names in the seed (per the uniqueness decision).
- A food with implausible values (guard the seed against obvious errors).

## Fixtures

- The seeded catalog itself.
- A minimal valid `FoodItem` for model-level unit tests.
