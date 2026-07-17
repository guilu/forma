# FOR-136 Test Plan

Strict TDD: failing tests first (aggregation/load → session resolution → API), then implement. No migration.

## Scope

The muscle-worked-map derivation for a strength session and its read endpoint. Reuses reference exercise data; no persistence.

## Domain / Application Tests

- Aggregating a template's exercises' `primaryMuscles` yields the union of worked muscles.
- Load level per muscle follows the documented frequency thresholds (e.g. ≥2 exercises → HIGH, 1 → MEDIUM); assert exact boundaries.
- A muscle hit by several exercises → HIGH; a muscle hit once → MEDIUM/base level.
- Resolution reuses `WorkoutTemplateService`/`ExerciseCatalog`/the schedule (assert against their real output; no duplicated resolution).

## API Tests

- `GET /training/sessions/{strength id}/muscle-map` → muscles + loads per `api.md`, derived from the session's exercises.
- Non-strength (running/rest) session id → 200 with `muscles: []`.
- Unknown session id → 404.
- Response shape matches `api.md` (sessionId + muscles[muscle,load]).

## Edge Cases

- Single-exercise session → that exercise's muscles at base load.
- Session whose exercises share a muscle → that muscle escalates to HIGH per threshold.
- Rest day id → empty map, not 404.

## Fixtures

- A strength template with exercises whose `primaryMuscles` overlap (to exercise HIGH) and a single-exercise case.
- The real `ExerciseCatalog`/`WorkoutTemplateCatalog` as the source; a running/rest session id for the empty case.
- H2-in-PostgreSQL-mode with Flyway (no new migration; head stays V18) for the API integration path, matching FOR-129 style (or a `@WebMvcTest` slice if no persistence is touched).
