# FOR-101 API Spec

## Endpoints

Extends the existing FOR-17 body measurements read model (no new routes):

### GET /api/v1/body/measurements

Each measurement now carries a derived `bmiCategory` when a `bmi` is present.

## Request

No change (read-only extension).

## Response

`200 OK` (excerpt; `bmiCategory` omitted when `bmi` is null):

```json
{
  "measuredAt": "2026-07-11T08:00:00Z",
  "source": "MANUAL",
  "weightKg": 73.6,
  "bmi": 22.7,
  "bmiCategory": "SALUDABLE"
}
```

- `bmiCategory`: derived from `bmi`; one of the documented bands. Suggested
  (WHO adult) bands, thresholds documented in code:
  - `BAJO_PESO` — BMI < 18.5
  - `SALUDABLE` — 18.5 ≤ BMI < 25
  - `SOBREPESO` — 25 ≤ BMI < 30
  - `OBESIDAD` — BMI ≥ 30
- Descriptive label only — **not** medical advice or diagnosis.
- Absent when `bmi` is null.

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

No new input. Classification is derived from the existing validated `bmi`.
