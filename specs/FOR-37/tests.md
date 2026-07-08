# FOR-37 Test Plan

## Scope

Unit-test `ShoppingList` and `ShoppingListItem`: creation, product references,
check/uncheck, and validation.

## Domain Tests

- A `ShoppingList` with multiple `ShoppingListItem`s is created correctly.
- Each item references a product id and carries `quantity` and
  `estimatedCostEur`.
- An item can be checked and unchecked.
- `status` only accepts the defined enum values.
- Construction rejects invalid values (non-positive `quantity`, missing refs).

## Application Tests

N/A — no application/use-case layer is introduced by this story (persistence, if
added, is exercised via an adapter test when it lands with FOR-38/FOR-39).

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — the checklist UI is FOR-39.

## Edge Cases

- `quantity` zero/negative.
- Empty list (no items) — valid.
- Cost stored vs. derived (assert per the documented rule).

## Fixtures

- A weekly list with two checked/unchecked items referencing products.
- An empty weekly list for a given `weekStartDate`.
