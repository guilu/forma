# FOR-45 API Spec

Follows the shared conventions in `docs/api-conventions.md` (ADR-005) and the
`ApiError` baseline. Mounted under `ApiPaths.V1` (`/api/v1`).

## Endpoints

### GET /api/v1/insights/weekly

Returns the current week's insights: the check-in summary, the main
recommendation, secondary recommendations, and a generated timestamp. Combines
Body (FOR-21) and Training (FOR-28) data; handles empty data gracefully.

## Request

No request body or parameters (current week only in the MVP).

## Response

`200 OK`

```json
{
  "generatedAt": "2026-07-08T10:00:00Z",
  "checkIn": {
    "weekStartDate": "2026-07-06",
    "latestWeightKg": 73.6,
    "latestBodyFatPercentage": 14.7,
    "latestLeanMassKg": 62.8,
    "plannedRunningSessions": 3,
    "completedRunningSessions": 2,
    "plannedStrengthSessions": 3,
    "completedStrengthSessions": 3
  },
  "mainRecommendation": {
    "category": "BODY",
    "severity": "INFO",
    "message": "Mantén el plan esta semana.",
    "reason": "Peso estable y grasa corporal a la baja."
  },
  "secondaryRecommendations": [
    {
      "category": "TRAINING",
      "severity": "INFO",
      "message": "Buena adherencia al entrenamiento.",
      "reason": "5 de 6 sesiones completadas."
    }
  ]
}
```

Null check-in fields (missing data) are omitted. With no data, `mainRecommendation`
is the insufficient-data recommendation and `secondaryRecommendations` is empty.

## Errors

Standard `ApiError` shape:

- 500 Internal Server Error — `INTERNAL_ERROR`: unexpected failures only. There
  is no request input, so no `VALIDATION_ERROR`; empty data is a normal `200`.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

No request input to validate. The main recommendation is selected by a documented
priority (`ACTION` > `WARNING` > `INFO`).
