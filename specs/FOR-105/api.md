# FOR-105 API Spec

## Endpoints

### GET /api/v1/nutrition/days/{type}

Existing FOR-34 endpoint, **enriched** (additive, backward compatible) with the
FOR-32 computed macros: per-meal totals, day totals and the target comparison.
`{type}` is a `NutritionDayType` (e.g. `running`); unknown → 404. Computed on
demand, no persistence.

## Request

No body. `{type}` path variable (case-insensitive, as today).

## Response

`200 OK` (new fields marked — existing `type`/`targets`/`meals[].items` unchanged):

```json
{
  "type": "RUNNING",
  "targets": { "calories": 2300, "proteinG": 160, "carbsG": 250, "fatG": 70 },
  "totals": { "calories": 2290, "proteinG": 158.0, "carbsG": 248.5, "fatG": 69.0 },
  "targetComparison": {
    "caloriesReached": false,
    "proteinReached": false,
    "carbsReached": false,
    "fatReached": false
  },
  "meals": [
    {
      "mealType": "BREAKFAST",
      "name": "Desayuno",
      "preferredTime": "08:15",
      "optional": false,
      "totals": { "calories": 480, "proteinG": 32.0, "carbsG": 58.0, "fatG": 14.0 },
      "items": [ { "food": "Avena", "quantityG": 80 } ]
    }
  ]
}
```

New fields:
- `totals` (top level): the day's `NutritionTotals` (FOR-32 `dayTotals`).
- `targetComparison`: per-macro reached/short booleans (FOR-32 `compareToTargets`).
- `meals[].totals`: each meal's `NutritionTotals` (FOR-32 `mealTotals`).

`NutritionTotals`: `calories` (whole kcal, int), `proteinG`/`carbsG`/`fatG` (one
decimal, double). Carried as the service rounds them (no re-rounding, no fake
precision). These are PLAN macros vs target — not consumed/logged intake.

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 404 Not Found — `NOT_FOUND`: unknown day type (unchanged behavior).
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

No new input. `{type}` validation unchanged (unknown → 404).
