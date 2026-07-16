# FOR-134 API Spec

> No new endpoints. Extends the FOR-127 `GET /api/v1/nutrition/consumption` response with
> consumed key-nutrient totals, and the `POST /api/v1/nutrition/log` free-entry body with
> optional key nutrients. Confirm current shapes against `delivery/nutrition/*`.

## Endpoints

### GET /api/v1/nutrition/consumption?date=YYYY-MM-DD (extended)

Day consumption read model, now also carrying consumed key-nutrient totals.

### POST /api/v1/nutrition/log (extended, free entries)

Free/ad-hoc entries may optionally include key nutrients.

## Request

`POST /api/v1/nutrition/log` (free entry with optional key nutrients):
```json
{
  "date": "2026-07-16",
  "mealType": "SNACK",
  "name": "Barrita",
  "kcal": 180, "proteinG": 6, "carbsG": 24, "fatG": 7,
  "fiberG": 3, "sugarsG": 12, "sodiumMg": 90, "saturatedFatG": 2
}
```
(All key nutrients optional; catalog-food entries derive them from `FoodItem`.)

## Response

`GET /api/v1/nutrition/consumption?date=2026-07-16`
```json
{
  "date": "2026-07-16",
  "consumed": { "kcal": 1650, "proteinG": 120, "carbsG": 160, "fatG": 55 },
  "keyNutrients": { "fiberG": 22, "sugarsG": 40, "sodiumMg": 1800, "saturatedFatG": 12 },
  "target": { ... },
  "comparison": { ... },
  "entries": [ ... ]
}
```
- `keyNutrients` values are `null` (or the documented partial-total form) when contributing foods lack the data — never fabricated.
- Units: `sodiumMg` in milligrams; `fiberG`/`sugarsG`/`saturatedFatG` in grams.

## Errors

- 400 Bad Request — negative key-nutrient values on a free entry.
- Otherwise unchanged from FOR-127 (empty day → 200, never 404).

## Authorization

Single-user MVP (ADR-002), owner-scoped. Unchanged from FOR-127.

## Validation

- Optional key nutrients non-negative when present → else 400.
- No new required fields; catalog entries need no request input for key nutrients.
