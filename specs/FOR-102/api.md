# FOR-102 API Spec

> Endpoint shapes are proposals aligned with existing `delivery/nutrition` conventions
> (ADR-005). Confirm exact paths against `ApiPaths.java` at implementation time.

## Endpoints

### POST /api/v1/nutrition/log

Log a consumed meal entry for a day.

### GET /api/v1/nutrition/consumption?date=YYYY-MM-DD

Day consumption read model: consumed vs target macros + key nutrients.

### POST /api/v1/nutrition/hydration

Log a water-intake entry for a day.

### GET /api/v1/nutrition/hydration?date=YYYY-MM-DD

Hydration progress read model: total volume vs daily goal.

## Request

`POST /api/v1/nutrition/log`
```json
{
  "date": "2026-07-15",
  "mealType": "LUNCH",
  "foodItemId": "af12...",
  "portions": 1.5
}
```
Free/ad-hoc entry (no catalog food):
```json
{
  "date": "2026-07-15",
  "mealType": "SNACK",
  "name": "Café con leche",
  "kcal": 90,
  "proteinG": 5,
  "carbsG": 8,
  "fatG": 3
}
```

`POST /api/v1/nutrition/hydration`
```json
{ "date": "2026-07-15", "volumeMl": 500 }
```

## Response

`GET /api/v1/nutrition/consumption?date=2026-07-15`
```json
{
  "date": "2026-07-15",
  "consumed": { "kcal": 1650, "proteinG": 120, "carbsG": 160, "fatG": 55 },
  "target":   { "kcal": 2000, "proteinG": 150, "carbsG": 200, "fatG": 60 },
  "comparison": { "kcalDelta": -350, "withinTarget": true },
  "keyNutrients": { "fiberG": 22, "sugarsG": 40, "sodiumMg": null, "saturatedFatG": 12 },
  "entries": [ { "id": "…", "mealType": "LUNCH", "name": "…", "kcal": 600 } ]
}
```

`GET /api/v1/nutrition/hydration?date=2026-07-15`
```json
{ "date": "2026-07-15", "totalMl": 1500, "goalMl": 2000, "progress": 0.75 }
```

## Errors

- 400 Bad Request — invalid date, negative portions/volume, or unknown `mealType`/`foodItemId`.
- 404 Not Found — only if a required referenced resource is addressed by id and missing.
- Consumption/hydration GET before any log → 200 with empty entries / zero totals, never 404.

## Authorization

Single-user MVP (ADR-002): all resources are owner-scoped to the fixed owner constant. Do not bypass the owner boundary even though there is one user.

## Validation

- `date` required, ISO-8601, not in the far future.
- `portions` > 0; exactly one of `foodItemId` or free-item macros must be provided.
- `volumeMl` > 0.
- Unknown enum values (`mealType`) → 400 `VALIDATION_ERROR`, never coerced.
- Never accept or echo provider tokens or secrets (N/A here, but keep the response free of internal ids not needed by the UI).
