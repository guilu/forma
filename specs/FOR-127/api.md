# FOR-127 API Spec

> Meal-logging subset of `specs/FOR-102/api.md`, scoped to this slice (macros only, no
> hydration, no key nutrients). Shapes align with ADR-005 and the `delivery/nutrition`
> conventions. Confirm exact paths against `ApiPaths.java` at implementation time.

## Endpoints

### POST /api/v1/nutrition/log

Log a consumed meal entry for a day.

### GET /api/v1/nutrition/consumption?date=YYYY-MM-DD

Day consumption read model: consumed macros vs plan target.

## Request

`POST /api/v1/nutrition/log` — catalog food:
```json
{ "date": "2026-07-15", "mealType": "LUNCH", "foodItemId": "af12...", "portions": 1.5 }
```
Free/ad-hoc entry (no catalog food):
```json
{
  "date": "2026-07-15",
  "mealType": "SNACK",
  "name": "Café con leche",
  "kcal": 90, "proteinG": 5, "carbsG": 8, "fatG": 3
}
```

## Response

`POST /api/v1/nutrition/log`
```json
{ "id": "…", "date": "2026-07-15", "mealType": "LUNCH", "name": "…", "kcal": 600, "proteinG": 40, "carbsG": 60, "fatG": 20 }
```

`GET /api/v1/nutrition/consumption?date=2026-07-15`
```json
{
  "date": "2026-07-15",
  "consumed": { "kcal": 1650, "proteinG": 120, "carbsG": 160, "fatG": 55 },
  "target":   { "kcal": 2000, "proteinG": 150, "carbsG": 200, "fatG": 60 },
  "comparison": { "kcalDelta": -350, "withinTarget": true },
  "entries": [ { "id": "…", "mealType": "LUNCH", "name": "…", "kcal": 600 } ]
}
```
- `target`/`comparison` are `null` (or omitted) when the day has no plan target — consumed still returned.

## Errors

- 400 Bad Request — invalid/missing `date`, unknown `mealType`, neither `foodItemId` nor macros provided, negative `portions`, or unknown `foodItemId`.
- Consumption GET before any log → 200 with zeroed consumed / empty entries, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary. Entries are only visible to their owner.

## Validation

- `date` required, ISO-8601, not in the far future.
- Exactly one of `foodItemId` (+ `portions` > 0) or free-item macros must be provided.
- `mealType` must be a known enum value → else 400 `VALIDATION_ERROR`.
- Macro fields non-negative.
- Never coerce unknown enum values; never log entry contents.
