# FOR-129 API Spec

> Adherence subset of `specs/FOR-104/api.md`, scoped to this slice. Shapes align with
> ADR-005 and the merged read-model conventions. Confirm exact paths against `ApiPaths.java`.

## Endpoints

### GET /api/v1/progress/adherence?days=30

Per-category planned vs completed over a rolling window ending today.

## Request

`GET /api/v1/progress/adherence?days=30` — `days` optional, defaults to 30, bounded (e.g. 1–365).

## Response

```json
{
  "windowDays": 30,
  "from": "2026-06-17",
  "to": "2026-07-16",
  "categories": [
    { "category": "TRAINING",     "planned": 20, "completed": 17, "rate": 0.85 },
    { "category": "NUTRITION",    "planned": 30, "completed": 24, "rate": 0.80 },
    { "category": "MEASUREMENTS", "planned": 4,  "completed": 4,  "rate": 1.0 }
  ]
}
```
- `rate` is `null` when `planned` is 0 (documented) — never a divide-by-zero.
- NUTRITION `planned` is "days in window" and `completed` is "days with at least one logged entry" (MVP definition, documented).
- MEASUREMENTS `planned` is the expected count from the assumed cadence (e.g. weekly), `completed` the actual entries.

## Errors

- 400 Bad Request — `days` out of the bounded range or non-numeric.
- Empty data → 200 with zeroed categories, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary.

## Validation

- `days` numeric, within the bounded range → else 400 `VALIDATION_ERROR`.
- No request body.
