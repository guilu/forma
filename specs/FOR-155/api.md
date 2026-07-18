# FOR-155 API Spec

> New weekly-tracking-record endpoints. Confirm the exact path/segment against `ApiPaths.java`
> (`/api/v1`) and the body/measurements delivery package. Aligns with ADR-005.

## Endpoints

### GET /api/v1/tracking/weekly (confirm path)

List the user's weekly tracking records (newest first). Starts **empty**.

### POST /api/v1/tracking/weekly (confirm path)

Create/upsert a weekly record for a given week.

### GET /api/v1/tracking/weekly/{week} (confirm path)

Read a single week's record.

## Request (POST)

```json
{
  "week": 1,
  "date": "2026-07-06",
  "weightKg": 73.6,
  "bodyFatPct": 14.7,
  "fatMassKg": 10.8,
  "leanMassKg": 62.8,
  "bmi": 22.7,
  "runningKm": 13.0,
  "pace4kmMinPerKm": "6:00",
  "recommendedKcal": 2300,
  "comment": "…"
}
```
- Body-composition fields optional/partial; `recommendedKcal` may be derived (FOR-149) or supplied — document the chosen behavior.
- Field names to match the final DTO conventions.

## Response

- The persisted weekly record (same fields). List endpoint returns `[]` when empty (not 404).

## Errors

- 400 Bad Request — invalid values (negative weight, % out of range, malformed pace).
- 404 Not Found — `GET /{week}` for a week with no record.
- Empty collection is **200 + []**, never an error.

## Authorization

Single-user MVP (ADR-002), owner-scoped (fixed OWNER_ID); consistent with existing body/measurement endpoints.

## Validation

- Weight strictly positive; body-fat/percentages within [0,100] (BodyMeasurement precedent).
- Week key valid/positive; one record per week (dedupe/upsert behavior documented).
