# FOR-100 API Spec

## Endpoints

Extends the existing FOR-17 body measurements API (no new routes):

### POST /api/v1/body/measurements

Records a manual measurement; now accepts optional `muscleMassKg` and
`waterPercentage`.

### GET /api/v1/body/measurements

Lists measurements; now returns `muscleMassKg` / `waterPercentage` when present.

## Request

`POST` body (new fields optional):

```json
{
  "measuredAt": "2026-07-11T08:00:00Z",
  "weightKg": 73.6,
  "bodyFatPercentage": 14.7,
  "bmi": 22.7,
  "muscleMassKg": 62.8,
  "waterPercentage": 58.0,
  "notes": "Báscula Withings"
}
```

- `muscleMassKg`: optional, strictly positive when present.
- `waterPercentage`: optional, within `[0, 100]`.

## Response

`201 Created` / `200 OK` (fields omitted when null, per existing `@JsonInclude`):

```json
{
  "measuredAt": "2026-07-11T08:00:00Z",
  "source": "MANUAL",
  "weightKg": 73.6,
  "bodyFatPercentage": 14.7,
  "bmi": 22.7,
  "fatMassKg": 10.8,
  "leanMassKg": 62.8,
  "muscleMassKg": 62.8,
  "waterPercentage": 58.0,
  "notes": "Báscula Withings"
}
```

- `leanMassKg` remains DERIVED (weight × body-fat); `muscleMassKg` is the new
  MEASURED field — both may appear.

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 400 Bad Request — `VALIDATION_ERROR`: `waterPercentage` outside `[0,100]`, or
  non-positive `muscleMassKg`.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

`muscleMassKg` strictly positive when present; `waterPercentage` in `[0,100]`;
both optional. Existing fields validate as before.
