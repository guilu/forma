# FOR-161 API Spec

> Adds one read endpoint to the existing `delivery/training/TrainingController` (base
> `/api/v1/training`), exposing the `RunningPlanGenerator` output. Aligns with ADR-005. Confirm
> exact paths against `ApiPaths.java` and the controller before coding.

## Existing endpoints (for context — not changed)

- `GET /api/v1/training/week` — current week.
- `GET /api/v1/training/weekly-summary` — weekly summary.
- `GET /api/v1/training/sessions/{id}/muscle-map` — per-session muscle map.

## New endpoint (this story)

### GET /api/v1/training/running-plan

Return the full multi-week running plan computed by `RunningPlanGenerator` (progression + deloads).

## Request

- No parameters (the plan length is a domain constant, currently 16 weeks). If design later exposes a
  parameter, document its bounded range and default.
- No request body.

## Response

```json
{
  "weeks": [
    {
      "week": 1,
      "deload": false,
      "totalKm": 13.0,
      "sessions": [
        { "label": "Sesión 1", "distanceKm": 4.0, "description": "..." },
        { "label": "Sesión 2", "distanceKm": 4.0, "description": "6x400m" },
        { "label": "Sesión 3", "distanceKm": 5.0, "description": "..." }
      ]
    }
    // ... through week 16, deload weeks every 4th week
  ]
}
```

- Shape is indicative — finalise the per-week / per-session fields in design against
  `RunningPlanGenerator`'s actual output. The response is a DTO, never the domain object.
- Deload weeks carry `deload: true` (or an equivalent explicit marker) so the UI can highlight them
  without recomputing the deload rule.

## Errors

- Read-only endpoint over a pure generator — no client input, so no `VALIDATION_ERROR` for the
  no-parameter form. Unexpected failures surface via `GlobalExceptionHandler` as 500.

## Authorization

Single-user MVP (ADR-002); reference/plan data, owner-scoped posture consistent with the other
`/training` reads. No account/owner path segment or auth header accepted yet.

## Validation

- No request input to validate in the no-parameter form.
- The endpoint reuses `RunningPlanGenerator` — it must not re-implement progression/deload logic in
  the controller (AGENTS.md: no business rules in delivery).
