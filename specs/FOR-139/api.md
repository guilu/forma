# FOR-139 API Spec

> Streak & weekly-history subset of `specs/FOR-104/api.md`, scoped to this slice. Aligns with
> ADR-005. Mount on the existing `delivery/progress/ProgressController` (`/api/v1/progress`).
> Confirm exact paths against `ApiPaths.java` and `ProgressController`.

## Endpoints

### GET /api/v1/progress/streak

Current + longest consistency streak per the documented streak rule (spec.md).

### GET /api/v1/progress/weekly-history

Per-week series (bars) over a bounded window.

## Request

- `GET /api/v1/progress/streak` — no parameters (window is a documented constant, or optional `days`/`weeks` if design chooses; document the default).
- `GET /api/v1/progress/weekly-history` — optional `weeks` (bounded, default documented, e.g. 8–12).

## Response

`GET /api/v1/progress/streak`
```json
{ "currentStreakDays": 6, "longestStreakDays": 21, "asOf": "2026-07-18" }
```

`GET /api/v1/progress/weekly-history`
```json
{
  "weeks": [
    { "weekStart": "2026-05-25", "planned": 5, "completed": 4 },
    { "weekStart": "2026-06-01", "planned": 5, "completed": 5 }
  ]
}
```
- Empty history → `streak` all-zero; `weekly-history` all-zero buckets (series still present), never 404.
- Bucket shape (`planned`/`completed` vs a volume field) is finalized in design against docs/3-entrenamiento.png; document the chosen signal per bar.

## Errors

- 400 `VALIDATION_ERROR` — `weeks`/`days` out of the bounded range (if a parameter is exposed) or non-numeric, via `GlobalExceptionHandler`.
- Empty state → 200 zeroed payload, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped where the source supports it (nutrition via
`MealLogRepository`). Same documented owner-scoping gap as FOR-129 for tables without `owner_id`.
No account/owner path segment or auth header accepted yet.

## Validation

- Any exposed window parameter within a bounded range; out of range/non-numeric → 400.
- No request body.
