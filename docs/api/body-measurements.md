# Body measurements API

REST endpoints for body composition measurements (FOR-17). Built on the FOR-15
domain model and FOR-16 persistence, following the shared
[API conventions](../api-conventions.md) (ADR-005).

All endpoints live under the versioned base path `/api/v1` (`ApiPaths.V1`).

## `GET /api/v1/body/measurements`

Lists measurements ordered by `measuredAt` descending. Returns an empty array
when there are none (never a 404/error).

`200 OK`

```json
[
  {
    "measuredAt": "2026-07-05T08:00:00Z",
    "source": "MANUAL",
    "weightKg": 78.4,
    "bodyFatPercentage": 18.2,
    "bmi": 23.9,
    "fatMassKg": 14.27,
    "leanMassKg": 64.13,
    "notes": "Morning, fasted"
  }
]
```

`fatMassKg` and `leanMassKg` are derived by the domain model, not stored or
recomputed in the API layer. Null fields are omitted from the JSON.

## `POST /api/v1/body/measurements`

Records a manually entered measurement. `source` is **not** client-supplied —
the server always records API entries as `MANUAL` (extra `source` in the body is
ignored). Returns the created measurement (same shape as one list item above).

Request:

```json
{
  "measuredAt": "2026-07-05T08:00:00Z",
  "weightKg": 78.4,
  "bodyFatPercentage": 18.2,
  "bmi": 23.9,
  "notes": "Morning, fasted"
}
```

`201 Created` with the created measurement in the body.

### Validation

`notes` is optional; every other field is required. Bounds match the FOR-15
domain contract and are enforced at the API boundary via Bean Validation:

| Field | Rule |
| --- | --- |
| `measuredAt` | required, ISO-8601 timestamp |
| `weightKg` | required, strictly positive |
| `bodyFatPercentage` | required, within `[0, 100]` |
| `bmi` | required, strictly positive |
| `notes` | optional, free text |

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape. A
missing or out-of-range field returns `400` with `code: VALIDATION_ERROR` and a
per-field `details` entry:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "correlationId": "abc-123",
  "details": [{ "field": "weightKg", "message": "must not be null" }]
}
```

## Authorization

None enforced yet — FORMA is a single-user MVP; authentication is a later story
(ADR-002). No user/account scoping is applied.
