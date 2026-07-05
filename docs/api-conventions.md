# API Conventions

Baseline REST API conventions for FORMA (FOR-88, aligned with
[ADR-005](adr/ADR-005-api-design.md)). Product endpoints are added by their own
stories; this document defines the shared contract they follow.

## Versioned base path

All product endpoints live under a versioned base path:

```text
/api/v1
```

The base path is defined once in code as `ApiPaths.V1`
(`backend/.../delivery/ApiPaths.java`); controllers mount under it rather than
hardcoding the prefix. A new API version is introduced as a new base path, never
by breaking `/api/v1`.

## Smoke endpoint

A minimal, product-free smoke endpoint confirms the API is reachable:

```http
GET /api/v1/ping  ->  200 OK
{ "status": "ok" }
```

The Actuator health endpoint (`/actuator/health`, ADR-008) remains separate and
is used for container/orchestration health checks.

## Standard error response

Every error returns the same JSON shape (`ApiError`). Null fields are omitted.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "correlationId": "abc-123",
  "details": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

| Field | Meaning |
| --- | --- |
| `code` | Stable, machine-readable error code (see below). Clients branch on this, not on `message`. |
| `message` | Safe, human-readable summary. Never contains stack traces, secrets or internals. |
| `correlationId` | Per-request correlation id (from the `X-Correlation-Id` header, or generated). Set by `CorrelationIdFilter` and echoed on the response header; see [configuration](configuration.md#logging-and-correlation-ids). |
| `details` | Optional per-field validation problems (`field`, `message`). Present only for validation errors. |

### Error codes

| Code | HTTP | When |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Request body/params failed Bean Validation. |
| `NOT_FOUND` | 404 | Requested resource does not exist. |
| `UNAUTHORIZED` | 401 | Authentication required or failed. **Reserved placeholder** — auth flow is ADR-002 / a later story. |
| `FORBIDDEN` | 403 | Authenticated caller lacks permission. **Reserved placeholder.** |
| `INTERNAL_ERROR` | 500 | Unexpected server error. Full detail is logged server-side (keyed by `correlationId`); the client only ever sees the generic message. |

## Rules (from ADR-005)

- Validate all input at the API boundary (`@Valid` on request DTOs → `VALIDATION_ERROR`).
- Never expose stack traces or raw exception messages
  (`server.error.include-stacktrace: never`; the `GlobalExceptionHandler` owns
  responses).
- Never leak secrets, tokens or provider payloads.
- Do not reveal whether another user's private resource exists.
- Controllers stay thin and never return persistence entities directly.

## Where it lives

```text
backend/src/main/java/dev/diegobarrioh/forma/delivery/
  ApiPaths.java                 # versioned base path constant
  PingController.java           # smoke endpoint
  error/ApiError.java           # standard error response shape
  error/ApiErrorCode.java       # error code enum (incl. reserved auth placeholders)
  error/GlobalExceptionHandler.java  # @RestControllerAdvice mapping exceptions -> ApiError
```
