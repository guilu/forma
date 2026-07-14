# FOR-117 Test Plan

## Scope

Verify the richer item display (unit, servings) and the restored
"Generada"/"Porciones" tiles render correctly from the FOR-108 fields.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-108 contract; no backend change in this story.

## UI Tests

- Item row renders quantity with its unit (e.g. "14 ud").
- Item with `servings` present shows the servings detail; item with
  `servings: null` does not.
- Summary tiles include a "Generada" tile showing the list's `generatedAt`,
  formatted consistently with other date displays in the app.
- A pre-migration/backfilled `generatedAt` still renders a valid date, not
  a blank tile.
- An unrecognized `unit` value renders as raw text rather than crashing.

## Edge Cases

- Empty list (`items.length === 0`) → summary tiles (including the new
  ones) still render sensibly, no crash from an empty aggregate.
- Mixed food/non-food items → aggregate "Porciones" tile (if implemented)
  reflects only the applicable items, or is omitted per the documented
  decision in `spec.md`.

## Fixtures

- A shopping list fixture with items spanning: a unit + servings, a unit
  with no servings, and an unrecognized/unexpected unit value.
