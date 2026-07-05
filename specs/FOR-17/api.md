# FOR-17 API Spec

## Endpoints

### GET /api/v1/body/measurements

Lists body measurements ordered by `measuredAt` descending, including
provider `source` and derived mass values.

### POST /api/v1/body/measurements

Creates a manually entered measurement (`source` is always set to `MANUAL`
by the server).

## Request

`POST /api/v1/body/measurements`

```json
{
  "measuredAt": "2026-07-05T08:00:00Z",
  "weightKg": 78.4,
  "bodyFatPercentage": 18.2,
  "bmi": 23.9,
  "notes": "Morning, fasted"
}
```

`notes` is optional; all other fields are required (see spec.md Functional
Requirements).

## Response

`GET /api/v1/body/measurements` — `200 OK`

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

`POST /api/v1/body/measurements` — `201 Created`, same object shape as one
list item above.

## Errors

Standard `ApiError` shape (docs/api-conventions.md):

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "correlationId": "abc-123",
  "details": [{ "field": "weightKg", "message": "must not be null" }]
}
```

- 400 Bad Request — `VALIDATION_ERROR`: missing/invalid required field.
- 401 Unauthorized — reserved placeholder (docs/api-conventions.md); not
  enforced in this story (single-user MVP, no auth yet).
- 403 Forbidden — reserved placeholder; not enforced in this story.
- 404 Not Found — not applicable to these two endpoints (no path parameter).
- 500 Internal Server Error — `INTERNAL_ERROR`, unexpected failures only;
  never exposes internals to the client.

## Authorization

None enforced yet — FORMA is a single-user MVP and auth is a later story
(ADR-002, referenced as a reserved placeholder in docs/api-conventions.md).
Do not add ad-hoc auth checks here; do not assume any user/account scoping
beyond what already exists in the repository.

## Validation

- `measuredAt`: required, ISO-8601 timestamp.
- `weightKg`: required, positive number.
- `bodyFatPercentage`: required, numeric in `[0, 100]`.
- `bmi`: required, positive number.
- `notes`: optional, free text.
- All validation performed via Bean Validation (`@Valid`) on the request DTO,
  mapped to `VALIDATION_ERROR` by the existing `GlobalExceptionHandler`
  (FOR-88) — do not hand-roll a separate validation/error path.
