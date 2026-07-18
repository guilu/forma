# FOR-153 API Spec

> No contract change. The existing running-plan read model now reflects the real curve, 6×400m and
> deload weeks. Confirm the path against `ApiPaths.java` (`/api/v1`) and the training/running
> controller. Aligns with ADR-005.

## Endpoints

### GET /api/v1/training/running-plan (existing — confirm path)

The 16-week running plan. After this slice: weekly volume 13→19 km, session 2 = 6×400m (except
deload weeks 4/8/12/16), long run 5.0→10.0 km, sessions on Mon/Wed/Sat (FOR-151).

## Request

`GET` — no body. Owner is the fixed OWNER_ID (ADR-002).

## Response

- Same `RunningPlanSession` shape as today (week, day, type, distance, effort, notes), values updated.
- If a structured interval descriptor is added for 6×400m, it appears as an additional optional field consistent with the existing DTO; otherwise the 6×400m detail rides in `notes`.

## Errors

- No new error cases. No client input beyond auth context.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with existing training endpoints.

## Validation

- No request body. Distance/effort validated by the `RunningPlanSession` domain invariants.
