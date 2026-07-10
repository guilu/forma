# Weekly insights API

Read-only endpoint backing the dashboard's weekly status + guidance (FOR-45).
Follows the shared [API conventions](../api-conventions.md) (ADR-005). Lives
under `/api/v1`.

## `GET /api/v1/insights/weekly`

Returns the current week's insights: the FOR-40 check-in snapshot (body + training),
a prioritized **main** recommendation, any **secondary** recommendations, and a
**generated timestamp**. Computed on demand — no persisted insights.

The recommendations come from the FOR-42 (body trend), FOR-43 (training adherence)
and FOR-44 (recovery warning) rule sets. The **main** one is chosen by severity
priority `ACTION > WARNING > INFO`; ties keep production order (body, then training,
then recovery). Empty underlying data still returns `200` with an insufficient-data
`INFO` recommendation as the main one — never an error.

Body values and `relatedMetric`/`notes` are omitted when absent (no fake precision).

`200 OK`

```json
{
  "checkIn": {
    "weekStartDate": "2026-07-06",
    "latestWeightKg": 70.0,
    "latestBodyFatPercentage": 18.0,
    "latestLeanMassKg": 55.0,
    "plannedRunningSessions": 3,
    "completedRunningSessions": 3,
    "plannedStrengthSessions": 3,
    "completedStrengthSessions": 2
  },
  "main": {
    "category": "BODY",
    "severity": "ACTION",
    "message": "El peso baja rápido; considera aumentar un poco las calorías para frenar la pérdida.",
    "reason": "El peso baja 1.5 kg en 7 días (~-2.1% por semana), por encima del 1% semanal recomendado.",
    "relatedMetric": "weeklyWeightChangeKg",
    "createdAt": "2026-07-10T08:00:00Z"
  },
  "secondary": [
    {
      "category": "TRAINING",
      "severity": "INFO",
      "message": "Semana muy constante; mantén este ritmo.",
      "reason": "Se completaron 5 de 6 sesiones planificadas.",
      "createdAt": "2026-07-10T08:00:00Z"
    }
  ],
  "generatedAt": "2026-07-10T08:00:00Z"
}
```

- `checkIn`: the FOR-40 weekly snapshot; absent body values are omitted.
- `main`: the highest-priority recommendation (`ACTION > WARNING > INFO`).
- `secondary`: remaining recommendations in priority order; empty when there is only one.
- `category`: `BODY` | `TRAINING` | `NUTRITION` | `RECOVERY` | `SHOPPING` (FOR-41).
- `severity`: `INFO` | `WARNING` | `ACTION`.
- `relatedMetric`: optional light metric reference; omitted when absent.
- `generatedAt`: when the insights were computed.

Only Body (FOR-21) and Training (FOR-28) data feed the insights in this iteration;
nutrition and shopping may be added later.

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 500 — `INTERNAL_ERROR`: unexpected failures only. Absent data is not an error.

## Authorization

None enforced yet — single-user MVP (ADR-002).
