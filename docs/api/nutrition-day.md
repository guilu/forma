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

`200 OK`

```json
{
  "type": "RUNNING",
  "targets": { "calories": 1940, "proteinG": 162, "carbsG": 271, "fatG": 25 },
  "meals": [
    {
      "mealType": "BREAKFAST",
      "name": "Desayuno",
      "preferredTime": "08:00",
      "optional": false,
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
      "items": [{ "food": "Proteína whey", "quantityG": 20 }]
    }
  ]
}
```

- `optional`: `true` for the post-run recovery meal.
- `items[].food`: resolved food name from the FOR-30 catalog.

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 404 Not Found — `NOT_FOUND`: unknown day type.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).
