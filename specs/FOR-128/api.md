# FOR-128 API Spec

> This story does NOT add endpoints. It changes the response of the EXISTING FOR-127
> endpoint `GET /api/v1/nutrition/consumption?date=` so `target`/`comparison` populate
> instead of being null. Confirm the current shape against `delivery/nutrition/DayConsumptionResponse.java`.

## Endpoints

### GET /api/v1/nutrition/consumption?date=YYYY-MM-DD (modified)

Day consumption read model. After this story, `target` and `comparison` are derived
from the date's resolved `NutritionDayType` instead of being null.

## Request

Unchanged: `GET /api/v1/nutrition/consumption?date=2026-07-15`

## Response

Before (FOR-127):
```json
{
  "date": "2026-07-15",
  "consumed": { "kcal": 1650, "proteinG": 120, "carbsG": 160, "fatG": 55 },
  "target": null,
  "comparison": null,
  "entries": [ ... ]
}
```
After (FOR-128) — 2026-07-15 is a Wednesday → STRENGTH day → strength `NutritionDayTemplate` target:
```json
{
  "date": "2026-07-15",
  "dayType": "STRENGTH",
  "consumed": { "kcal": 1650, "proteinG": 120, "carbsG": 160, "fatG": 55 },
  "target":   { "kcal": 2000, "proteinG": 150, "carbsG": 200, "fatG": 60 },
  "comparison": { "kcalDelta": -350, "withinTarget": true },
  "entries": [ ... ]
}
```
- Adding `dayType` to the response is recommended (so the UI can label the day) but optional — document if included.

## Errors

- Unchanged from FOR-127: invalid/missing `date` → 400; empty day → 200 (now with populated target).
- No new error modes.

## Authorization

Unchanged: single-user MVP (ADR-002), owner-scoped.

## Validation

- Unchanged `date` validation from FOR-127.
- No new request fields.
