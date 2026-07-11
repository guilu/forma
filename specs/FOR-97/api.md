# FOR-97 API Spec

## Endpoints

### GET /api/v1/body/weekly-summary

Returns the current weekly body-composition summary (FOR-21): latest weight /
body fat / lean mass plus the weekly change since the previous measurement.
Computed on demand; no persistence.

## Request

No body, no parameters (current summary only).

## Response

`200 OK` (populated):

```json
{
  "latestWeightKg": 73.6,
  "latestBodyFatPercentage": 14.7,
  "latestLeanMassKg": 62.8,
  "weeklyWeightChangeKg": -0.3,
  "weeklyBodyFatChange": -0.6,
  "comparisonDays": 7,
  "message": "Peso 73.6 kg (-0.3 kg en 7 días). Grasa corporal 14.7% (-0.6%). Masa magra 62.8 kg."
}
```

Fewer than two measurements (deltas unavailable):

```json
{
  "latestWeightKg": 73.6,
  "latestBodyFatPercentage": 14.7,
  "latestLeanMassKg": 62.8,
  "weeklyWeightChangeKg": null,
  "weeklyBodyFatChange": null,
  "comparisonDays": null,
  "message": "Última medición — Peso 73.6 kg, grasa 14.7%, masa magra 62.8 kg. Registra otra medición para ver el cambio."
}
```

No measurements → all numeric fields null with an informative message.

- Delta fields are `null` (never `0`) when there is no prior measurement.
- `comparisonDays` is the actual number of days between the two most recent
  measurements (no fake "one week").

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 500 — `INTERNAL_ERROR`: unexpected failures only. No data is not an error.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

No input to validate (read-only, no parameters).
