# FOR-135 API Spec

> Achievements subset of `specs/FOR-104/api.md`, scoped to this slice. Aligns with ADR-005
> and the merged read-model conventions. Confirm exact paths against `ApiPaths.java`.

## Endpoints

### GET /api/v1/progress/achievements

Earned + available achievements. Evaluation on GET persists any newly-earned ones.

## Request

`GET /api/v1/progress/achievements` — no params.

## Response

```json
{
  "earned": [
    { "id": "FIRST_MEASUREMENT", "title": "Primera medición", "description": "…", "earnedAt": "2026-07-10T08:00:00Z" }
  ],
  "available": [
    { "id": "TEN_MEALS_LOGGED", "title": "10 comidas registradas", "description": "…" }
  ]
}
```
- `earned` entries carry `earnedAt`; `available` entries do not.
- Empty state (nothing earned yet) → `earned: []`, `available` = full catalog. Never 404.

## Errors

- No client input to validate. Empty data → 200 with empty `earned`.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary.

## Validation

- No request body/params.
- Evaluation is deterministic and idempotent; a newly-earned achievement is persisted exactly once (PK owner_id, achievement_id).
