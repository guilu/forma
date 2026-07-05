# FOR-17: Create body measurements API

Jira: https://dbhlab.atlassian.net/browse/FOR-17
Epic: FOR-2 Body Composition

## Summary

Expose `BodyMeasurement` (FOR-15/FOR-16) through REST: `GET
/api/v1/body/measurements` and `POST /api/v1/body/measurements`. Request/response
DTOs stay separate from the persistence model; validation errors follow the
existing `ApiError` shape.

## User/System Flow

1. Client calls `POST /api/v1/body/measurements` with a manual measurement.
2. Controller validates the request DTO, builds a domain `BodyMeasurement`
   (`source = MANUAL`), saves it via the FOR-16 repository, returns the
   created measurement (including derived values) as a response DTO.
3. Client calls `GET /api/v1/body/measurements` to list measurements ordered
   by `measuredAt` descending (FOR-16 default order), each including derived
   `fatMassKg`/`leanMassKg`.

## Functional Requirements

- Mount both endpoints under the existing versioned base path `ApiPaths.V1`
  (`/api/v1`, see `backend/.../delivery/ApiPaths.java`), per
  docs/api-conventions.md / ADR-005. The Jira summary writes the path as
  `/api/body/measurements`; this spec applies the already-established
  `/api/v1` prefix instead of introducing an unversioned endpoint —
  documented here as an explicit adaptation to existing convention, not new
  scope.
- `POST /api/v1/body/measurements` request DTO: `measuredAt`, `weightKg`,
  `bodyFatPercentage`, `bmi`, `notes`. `source` is not client-supplied; the
  controller/use case sets it to `MANUAL`.
- `GET /api/v1/body/measurements` response DTO (list): `measuredAt`,
  `source`, `weightKg`, `bodyFatPercentage`, `bmi`, `fatMassKg`,
  `leanMassKg`, `notes`.
- Required fields: `measuredAt`, `weightKg`, `bodyFatPercentage`, `bmi` (these
  are not marked optional in docs/domain-model.md, unlike `notes`). Confirm
  during implementation if product feedback narrows this further.
- Validation failures return `VALIDATION_ERROR` (HTTP 400) using the existing
  `ApiError`/`GlobalExceptionHandler` baseline
  (`backend/.../delivery/error/`), with per-field `details`.
- DTOs are distinct types from the FOR-15 domain model and the FOR-16
  persistence row — controllers never return persistence entities directly
  (ADR-005).

## Non-Functional Requirements

- Follow docs/api-conventions.md error shape and codes exactly (no ad-hoc
  error formats).
- No stack traces or internal details in responses
  (`server.error.include-stacktrace: never`, already configured per
  docs/api-conventions.md).
- No health data logged beyond what the existing correlation-id/structured
  logging baseline (FOR-91) already covers.

## Data Model Notes

Request/response DTOs map 1:1 to the fields listed above; derived
`fatMassKg`/`leanMassKg` are computed by the FOR-15 domain type, not
duplicated in the DTO/controller layer.

## Edge Cases

- Missing required field (e.g. `weightKg` absent) — 400 with a `details`
  entry naming the field.
- `bodyFatPercentage` out of a sane range (e.g. negative or > 100) — treat as
  a validation error, not a silent clamp.
- Empty list (`GET` with zero measurements) — return an empty array, not an
  error (dashboard/graph stories rely on this, see FOR-19/FOR-20).

## Open Questions

- Exact numeric validation bounds for `bodyFatPercentage`/`bmi`/`weightKg`
  are not specified by Jira; pick reasonable bounds (e.g.
  `bodyFatPercentage` in `[0, 100]`) and document them in code/tests rather
  than leaving them unbounded.
