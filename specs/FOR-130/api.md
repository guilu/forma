# FOR-130 API Spec

> Hydration subset of `specs/FOR-102/api.md`, scoped to this slice. Shapes align with
> ADR-005 and the `delivery/nutrition` conventions. Confirm exact paths against `ApiPaths.java`.

## Endpoints

### POST /api/v1/nutrition/hydration

Log a water-intake entry for a day.

### GET /api/v1/nutrition/hydration?date=YYYY-MM-DD

Hydration progress read model: total volume vs daily goal.

## Request

`POST /api/v1/nutrition/hydration`
```json
{ "date": "2026-07-16", "volumeMl": 500 }
```

## Response

`POST /api/v1/nutrition/hydration`
```json
{ "id": "…", "date": "2026-07-16", "volumeMl": 500 }
```

`GET /api/v1/nutrition/hydration?date=2026-07-16`
```json
{ "date": "2026-07-16", "totalMl": 1500, "goalMl": 2000, "progress": 0.75 }
```
- `goalMl` comes from `DefaultObjectives.dailyWaterMl`, else the documented fallback default.
- `progress = totalMl / goalMl`; `null` if the goal cannot be determined (documented). Cap at 1.0 or report raw — document.

## Errors

- 400 Bad Request — missing/invalid/far-future `date`, or `volumeMl` <= 0.
- Hydration GET before any log → 200 with `totalMl: 0` (goal/progress still resolved), never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary. Entries are only visible to their owner.

## Validation

- `date` required, ISO-8601, not in the far future.
- `volumeMl` required, > 0.
- Never log volumes; keep responses free of internal fields the UI doesn't need.
