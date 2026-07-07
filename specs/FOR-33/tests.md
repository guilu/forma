# FOR-33 Test Plan

## Scope

Validate the seeded nutrition day templates: existence, meals, referential
integrity, and macros approximately on target.

## Domain Tests

- A RUNNING, a STRENGTH and a REST day template exist.
- Each template has at least one meal, each meal at least one item.
- Every `MealItem` references an existing FOR-30 `FoodItem` (fail fast on a bad
  id).

## Application Tests

- Each template's computed totals (FOR-32) are within a documented band of its
  targets (calories and protein especially).
- Running-day carbohydrates are higher than rest-day carbohydrates.
- If persisted, seeding is deterministic and idempotent.

## API Tests

N/A — no HTTP endpoint is defined by this story.

## UI Tests

N/A — no frontend in this story (the running-day flow UI is FOR-34).

## Edge Cases

- A seeded meal referencing a missing food id → build fails fast.
- Totals drifting outside the target band → seed adjusted until near target.

## Fixtures

- The seeded FOR-30 catalog plus the three generated day templates are the
  primary fixtures; assertions run against their structure and totals.
