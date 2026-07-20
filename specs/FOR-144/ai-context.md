# FOR-144 AI Context

## Story

FOR-144 — Progreso: progress-photo upload + gallery UI (consume FOR-140). Frontend-only.
**SHIPPED** in commit `1ab9d95` (PR #140). This context documents the delivered change.

## Intent

Let the user upload, view and delete progress photos against the private FOR-140 backend, keeping the
privacy-sensitive binaries behind the owner-scoped, access-controlled endpoint.

## Relevant Documents

- `specs/FOR-140/` — private progress-photo storage + owner-scoped retrieval.
- `specs/FOR-52/` — body composition / measurements screens.
- `AGENTS.md` — never log/leak sensitive data; frontend consumes endpoints.
- `docs/adr/ADR-002`, `ADR-006-frontend.md`, `docs/6-progreso.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-144

## Repo Notes (verified — as shipped)

- `frontend/src/pages/progress/ProgressPhotosSection.tsx` (+ `.module.css`, `.test.tsx`).
- Endpoints: `POST/GET/DELETE /api/v1/progress/photos`, `GET /api/v1/progress/photos/{id}` (binary).
- FOR-60 states reused; empty gallery is a normal state.

## Architectural Constraints

- Frontend-only; the photo binary is fetched only through the owner-scoped endpoint — never a public URL,
  never cached in a shared context.
- Client-side accept `image/jpeg,image/png`; backend enforces 5 MB / type (400 `VALIDATION_ERROR`).
- Accessible states.

## Common Pitfalls (avoided)

- Caching or exposing the photo binary in a public/shared context.
- Treating an empty gallery as an error.
- Relying on client-side validation alone (backend is authoritative on size/type).

## Validation

Frontend checks pass (`npm run test`, `typecheck`, `lint`, `build`); upload/list/delete work against
mocked endpoints, oversized/invalid uploads surface the 400, and the empty gallery renders as a normal
state. Delivered.
