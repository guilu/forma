# FOR-38 Test Plan

## Scope

Verify weekly and monthly shopping budget calculation, and that it reflects
quantity/price changes.

## Domain Tests

- Weekly total equals the sum of the list's item costs.
- Monthly estimate equals weekly × 4.33.
- Changing an item quantity or a product price changes the computed budget.
- Rounding sums raw and rounds once for display (no accumulated error).

## Application Tests

- The budget service (if introduced) computes weekly/monthly for a realistic
  list reading current product prices.

## API Tests

N/A — no HTTP endpoint is required by this story.

## UI Tests

N/A — currency display is FOR-39.

## Edge Cases

- Empty list → weekly and monthly = 0.
- Item whose product has no/zero price (per the documented rule → treated as 0).
- Boundary rounding of the monthly ×4.33 factor.

## Fixtures

- A weekly list with a few items and known product prices → hand-computable
  weekly total and monthly estimate.
- An empty list.
