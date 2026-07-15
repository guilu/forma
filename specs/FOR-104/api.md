# FOR-104 API Spec

> Proposed shapes aligned with ADR-005, one group per proposed slice. Progress-photo
> endpoints never return a public URL. Confirm exact paths against `ApiPaths.java`.

## Endpoints

### Goals (slice 1)
- `GET /api/v1/goals` — list goals with progress + milestones.
- `POST /api/v1/goals` — create a goal (with milestones).
- `PATCH /api/v1/goals/{id}` — update a goal / milestone state.

### Adherence (slice 2)
- `GET /api/v1/progress/adherence?days=30` — planned vs completed per category.

### Streak & history (slice 3)
- `GET /api/v1/progress/streak` — current streak.
- `GET /api/v1/progress/weekly-history` — weekly-history bars.

### Achievements (slice 4)
- `GET /api/v1/progress/achievements` — earned + available achievements.

### Muscle map (slice 5)
- `GET /api/v1/training/sessions/{id}/muscle-map` — worked-muscle data for a strength session.

### Progress photos (slice 6)
- `POST /api/v1/progress/photos` — upload (multipart); returns a private reference id.
- `GET /api/v1/progress/photos` — list private references (metadata only).
- `GET /api/v1/progress/photos/{id}` — owner-scoped, access-controlled binary retrieval.
- `DELETE /api/v1/progress/photos/{id}` — delete.

## Request

`POST /api/v1/goals`
```json
{
  "title": "Bajar a 12% grasa",
  "metric": "BODY_FAT_PCT",
  "target": 12.0,
  "dueDate": "2026-12-31",
  "milestones": [ { "title": "15%", "target": 15.0 } ]
}
```

## Response

`GET /api/v1/progress/adherence?days=30`
```json
{
  "windowDays": 30,
  "categories": [
    { "category": "TRAINING",    "planned": 20, "completed": 17, "rate": 0.85 },
    { "category": "NUTRITION",   "planned": 30, "completed": 24, "rate": 0.80 },
    { "category": "MEASUREMENTS","planned": 4,  "completed": 4,  "rate": 1.0 }
  ]
}
```
`GET /api/v1/progress/streak`
```json
{ "currentStreakDays": 6, "longestStreakDays": 21, "asOf": "2026-07-15" }
```
`GET /api/v1/training/sessions/{id}/muscle-map`
```json
{ "sessionId": "…", "muscles": [ { "muscle": "QUADRICEPS", "load": "HIGH" }, { "muscle": "GLUTES", "load": "MEDIUM" } ] }
```

## Errors

- 400 Bad Request — invalid goal metric/target, `days` out of range, malformed upload.
- 403 Forbidden — accessing another owner's photo/goal (do not bypass even in single-user MVP).
- 404 Not Found — unknown goal/session/photo id.
- Empty states (no goals, no planned items, no photos) → 200 with empty/zeroed payloads, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Progress photos are strictly owner-only; retrieval is access-controlled, never a public/static URL.

## Validation

- Goal `metric` must be a known enum; `target` numeric; `dueDate` ISO-8601 if present.
- `days` within a bounded range (e.g. 1–365).
- Photo upload: content-type and size limits enforced; content never logged.
- Unknown enum values → 400 `VALIDATION_ERROR`.
