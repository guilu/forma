# FOR-125 API Spec

> Goals subset of `specs/FOR-104/api.md`, scoped to this slice. Shapes align with
> ADR-005 and the FOR-107/108/109/110 delivery conventions. Confirm exact paths
> against `ApiPaths.java` at implementation time.

## Endpoints

### GET /api/v1/goals

List goals with derived progress and milestones, owner-scoped.

### POST /api/v1/goals

Create a goal with optional milestones.

### PATCH /api/v1/goals/{id}

Update goal fields and/or milestone state.

## Request

`POST /api/v1/goals`
```json
{
  "title": "Bajar a 12% grasa",
  "metric": "BODY_FAT_PCT",
  "target": 12.0,
  "dueDate": "2026-12-31",
  "milestones": [
    { "title": "15%", "target": 15.0 },
    { "title": "13.5%", "target": 13.5 }
  ]
}
```

`PATCH /api/v1/goals/{id}`
```json
{
  "title": "Bajar a 11% grasa",
  "target": 11.0,
  "milestones": [ { "id": "…", "completed": true } ]
}
```

## Response

`GET /api/v1/goals`
```json
{
  "goals": [
    {
      "id": "…",
      "title": "Bajar a 12% grasa",
      "metric": "BODY_FAT_PCT",
      "target": 12.0,
      "dueDate": "2026-12-31",
      "status": "ACTIVE",
      "progress": { "current": 16.4, "target": 12.0, "ratio": 0.42, "source": "BODY_MEASUREMENT" },
      "milestones": [
        { "id": "…", "title": "15%", "target": 15.0, "completed": false }
      ]
    }
  ]
}
```
- `progress` is `null` (or `current: null`, `ratio: null`) when the metric has no linked source or no data yet — never fabricated.

## Errors

- 400 Bad Request — unknown/invalid `metric`, non-numeric `target`, malformed `dueDate`.
- 404 Not Found — `PATCH` of an unknown goal id.
- Empty list (no goals) → 200 with `"goals": []`, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary. A goal is only visible/editable by its owner.

## Validation

- `title` required, non-blank.
- `metric` must be a known enum value → else 400 `VALIDATION_ERROR`.
- `target` numeric; `dueDate` ISO-8601 if present.
- Milestone `target` numeric; `completed` boolean on PATCH.
- Unknown enum values never coerced.
