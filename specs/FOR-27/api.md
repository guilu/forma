# FOR-27 API Spec

Follows the shared conventions in `docs/api-conventions.md` (ADR-005) and the
FOR-88 `ApiError` baseline. Exact paths are indicative; align with the scheduled
session model chosen in `spec.md` Open Questions.

## Endpoints

### PATCH /api/v1/training/sessions/{id}/status

Updates the status (and optional note) of a scheduled training session (running
or strength). Mounted under `ApiPaths.V1`; controller stays thin (ADR-001).

## Request

`PATCH /api/v1/training/sessions/{id}/status`

```json
{
  "status": "COMPLETED",
  "notes": "Felt strong, easy pace"
}
```

`status` is required (`PLANNED` | `COMPLETED` | `SKIPPED`); `notes` is optional.

## Response

`200 OK` — the updated session with its new status:

```json
{
  "id": "…",
  "type": "RUNNING",
  "status": "COMPLETED",
  "notes": "Felt strong, easy pace"
}
```

## Errors

Standard `ApiError` shape (`docs/api-conventions.md`):

- 400 Bad Request — `VALIDATION_ERROR`: missing/invalid `status`.
- 401 Unauthorized — reserved placeholder; not enforced (single-user MVP).
- 403 Forbidden — reserved placeholder; not enforced.
- 404 Not Found — `NOT_FOUND`: no session with the given id.

## Authorization

None enforced yet — single-user MVP (ADR-002 reserved placeholder). No
per-user scoping beyond what exists in the repo.

## Validation

- `status`: required, one of `PLANNED` | `COMPLETED` | `SKIPPED`.
- `notes`: optional, free text.
- Bean Validation on the request DTO → `VALIDATION_ERROR` via the existing
  `GlobalExceptionHandler` (FOR-88); no hand-rolled validation path.
- Allowed transitions enforced in the domain/application layer, not the
  controller.
