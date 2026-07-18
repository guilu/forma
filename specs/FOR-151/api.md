# FOR-151 API Spec

> No contract change. The weekly training-schedule read model (and nutrition day-type) simply reflect
> the corrected days; stable session ids `<DAY>:<KIND>` shift to the new days. Confirm paths against
> `ApiPaths.java` (`/api/v1`) and the training/nutrition controllers. Aligns with ADR-005.

## Endpoints

### GET /api/v1/training/schedule (existing — confirm path)

Weekly training calendar. After this slice: strength on Tue/Thu/Sun (PUSH/PULL/LEGS), running on
Mon/Wed/Sat, rest on Fri. Session ids change with the days (e.g. `TUESDAY:STRENGTH`).

### GET /api/v1/nutrition/... day-type (existing — confirm path)

Nutrition day-type per date now resolves to the corrected running/strength/rest days via the shared
policy. No shape change.

## Request

`GET` — no body. Owner is the fixed OWNER_ID (ADR-002).

## Response

- Same shape as today; the values (which weekday is running/strength/rest and each session id) reflect the corrected mapping.
- Downstream: FOR-136 `GET /api/v1/training/sessions/{sessionId}/muscle-map` resolves the new strength session ids (Tue/Thu/Sun).

## Errors

- No new error cases. Unknown session id → existing 404 behavior.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with existing training/nutrition endpoints.

## Validation

- No request body. The day policy is validated in the domain, not the API layer.
