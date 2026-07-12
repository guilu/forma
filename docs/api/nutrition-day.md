# Nutrition day API

Read-only endpoint backing the nutrition day flow (FOR-34). Follows the shared
[API conventions](../api-conventions.md) (ADR-005). Lives under `/api/v1`.

## `GET /api/v1/nutrition/days/{type}`

Returns the seeded nutrition day (FOR-33) for a day type: its macro targets and
its meals in preferred-time order. `type` is `running`, `strength` or `rest`
(case-insensitive).

For a running day the meals form the late-run flow: breakfast → lunch → pre-run
snack → post-run recovery (optional) → light dinner. The post-run meal is flagged
`optional` so the UI can present it as skippable (skip if the daily protein
target is already met).

Since FOR-105 the response is enriched (additive, backward compatible) with the
FOR-32 computed macros: each meal's `totals`, the day's `totals`, and a
`targetComparison` of the day totals against `targets`. All three are delegated
to `NutritionCalculationService` — no macro math happens in the controller/DTO.
These are **PLAN** macros vs target, not consumed/logged intake (that remains the
FOR-102 stub).

`200 OK`

```json
{
  "type": "RUNNING",
  "targets": { "calories": 1940, "proteinG": 162, "carbsG": 271, "fatG": 25 },
  "totals": { "calories": 547, "proteinG": 36.3, "carbsG": 81.2, "fatG": 9.5 },
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
      "preferredTime": "08:00",
      "optional": false,
      "totals": { "calories": 467, "proteinG": 20.3, "carbsG": 79.6, "fatG": 8.3 },
      "items": [
        { "food": "Avena", "quantityG": 120 },
        { "food": "Plátano", "quantityG": 120 }
      ]
    },
    {
      "mealType": "POST_WORKOUT",
      "name": "Recuperación (opcional)",
      "preferredTime": "20:00",
      "optional": true,
      "totals": { "calories": 80, "proteinG": 16.0, "carbsG": 1.6, "fatG": 1.2 },
      "items": [{ "food": "Proteína whey", "quantityG": 20 }]
    }
  ]
}
```

- `optional`: `true` for the post-run recovery meal.
- `items[].food`: resolved food name from the FOR-30 catalog.
- `totals` (day and per-meal): `calories` whole kcal (int), `proteinG`/`carbsG`/
  `fatG` one decimal (double) — carried as the FOR-32 calculator rounds them, no
  re-rounding here.
- `targetComparison`: `true` when the day's totals reach (`>=`) the matching
  `targets` value, per macro.

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 404 Not Found — `NOT_FOUND`: unknown day type.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).
