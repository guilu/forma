# FOR-27 AI Context

## Story

FOR-27 — Mark training sessions as completed
(https://dbhlab.atlassian.net/browse/FOR-27)

## Intent

Capture training adherence: let the user mark planned runs and strength sessions
completed/skipped so later stories (FOR-28 summary, insights) can relate plan vs.
actuals. Success is a simple, persisted status change surfaced in the calendar.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training statuses)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-003-persistence.md`
- `docs/api-conventions.md`, `docs/api/body-measurements.md` (endpoint + error
  precedent from FOR-17/FOR-88)
- `specs/FOR-22/`, `specs/FOR-25/`, `specs/FOR-26/`
- Jira: https://dbhlab.atlassian.net/browse/FOR-27

## Domain Notes

- Status is a small closed set (`PLANNED | COMPLETED | SKIPPED`) → enum; extends
  the docs/domain-model.md PLANNED/COMPLETED set with SKIPPED (document it).
- The status-change rule (allowed transitions) is a domain/application concern,
  not controller logic (ADR-001).

## Architectural Constraints

- Backend: domain/application for the status change + a thin controller under
  `.../delivery/`, reusing the FOR-88 error baseline
  (`ApiError`/`GlobalExceptionHandler`) and `ApiPaths.V1`.
- Persistence via JDBC + Flyway (no ORM); additive migration only if needed
  (ADR-003).
- Frontend: mark actions in the FOR-26 calendar, calling `apiClient`
  (relative `/api/...`); reuse existing UI primitives + states (ADR-006).

## Common Pitfalls

- Putting the transition rule in the controller instead of the domain/app layer.
- Inventing a new error shape instead of `VALIDATION_ERROR`/`NOT_FOUND`.
- Requiring detailed workout logging (out of scope).
- Assuming a scheduled-session instance exists — confirm/define the minimal one
  (spec.md Open Questions).

## Suggested Implementation Order

1. Define the status enum + allowed transitions and the (minimal) scheduled
   session identity.
2. Backend: application status-change use case + thin endpoint (`api.md`) +
   persistence.
3. Frontend: mark completed/skipped from the calendar; reflect status.
4. Tests: backend status change (happy/NOT_FOUND/validation) + frontend
   interaction.

## Validation

Run `./gradlew test` (backend) and `npm run build` / `npm run test` (frontend),
per AGENTS.md Verification guidance.
