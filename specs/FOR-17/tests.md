# FOR-17 Test Plan

## Scope

Verify the `GET`/`POST /api/v1/body/measurements` controller: happy paths,
validation errors, and correct DTO shape (no persistence/domain leakage).

## Domain Tests

N/A — covered by FOR-15.

## Application Tests

- Use case (if introduced) correctly delegates to the FOR-16 repository and
  builds a `MANUAL` measurement on create.

## API Tests

- `POST /api/v1/body/measurements` with a valid body creates a measurement
  and returns it with derived `fatMassKg`/`leanMassKg`.
- `POST` with a missing required field returns 400 with `VALIDATION_ERROR`
  and a `details` entry for that field.
- `GET /api/v1/body/measurements` returns measurements ordered by
  `measuredAt` descending.
- `GET` with zero measurements returns an empty array, not an error.
- Response DTOs never expose persistence-only fields (e.g. internal row id
  format) beyond what the contract in `api.md` defines.

## UI Tests

N/A — the UI arrives in FOR-18/FOR-19/FOR-20.

## Edge Cases

- `bodyFatPercentage` outside `[0, 100]`.
- `weightKg` non-positive.
- Extra/unknown fields in the request body (decide: ignored vs. rejected —
  document the chosen behavior in the test).

## Fixtures

- A valid `POST` payload.
- An invalid payload missing `weightKg`.
- A pre-seeded set of measurements (via FOR-16 repository) to assert `GET`
  ordering.
