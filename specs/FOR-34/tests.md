# FOR-34 Test Plan

## Scope

Verify the running-day meal flow: template composition/order (backend) and the
Nutrition page rendering of the flow (frontend).

## Domain Tests

- The RUNNING template includes a pre-run snack and an optional post-run
  recovery item.
- Dinner is lighter (fewer calories) than lunch.
- The running-day macro totals (FOR-32) still reach the target range.

## Application Tests

- If a data source/service is introduced, it returns the running-day flow in the
  intended meal order.

## API Tests

- Only if a read endpoint is added for the day flow: it returns the ordered
  meals and macro totals.

## UI Tests

- The Nutrition page renders the running-day meals in order (breakfast → lunch →
  pre-run snack → post-run → light dinner).
- The post-run recovery item is presented as optional.
- A short explanation of the structure (carbs earlier, lighter dinner) is shown.
- Loading/empty/error states render (ADR-006).

## Edge Cases

- Protein target already met → optional post-run item not implied mandatory.
- API/network failure → error state, not a crash.
- Long meal names/notes do not break the mobile layout.

## Fixtures

- The seeded RUNNING day template (FOR-33) with its meals and totals.
- A mocked running-day flow response for the frontend tests, plus an error case.
