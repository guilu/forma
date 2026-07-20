# FOR-144 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-144 (Jira status "Listo")
Epic: FOR-47 UI & UX
Backend: FOR-140. Frontend personalization batch.

## Status: SHIPPED (repo has priority over Jira)

Implemented and merged in commit `1ab9d95` — _"FOR-144 Progress-photo upload + gallery UI" (PR #140)_.
This spec documents the delivered behaviour retroactively.

## Summary

Frontend for progress photos: upload, list/gallery and delete, consuming the FOR-140 backend
(`POST/GET/DELETE /api/v1/progress/photos`, `GET /api/v1/progress/photos/{id}` for the owner-scoped
binary). Photos are privacy-sensitive personal data — the UI relies on the access-controlled,
owner-scoped endpoint and never a public URL. Frontend-only.

## Repository baseline (verified — as shipped)

- FOR-140 shipped the private, owner-scoped progress-photo backend.
- The UI lives at `frontend/src/pages/progress/ProgressPhotosSection.tsx` (+ styles + tests), within
  `ProgressPage`.

## Functional Requirements (delivered)

- Upload control: multipart `file`; client-side `accept="image/jpeg,image/png"`; surfaces the backend
  5 MB / type errors returned as `400 VALIDATION_ERROR`.
- Gallery: lists metadata via `GET /photos` and renders each image via `GET /photos/{id}` (owner-scoped
  binary, access-controlled — never a public URL).
- Delete a photo via `DELETE /photos/{id}`.
- FOR-60 loading/empty/error states; an empty gallery is a normal state, not an error.

## Non-Functional Requirements

- Privacy: the photo binary is never cached in a public/shared context; retrieval always goes through
  the owner-scoped endpoint.
- Accessible states; token-driven styling.

## Edge Cases (covered)

- Empty gallery → `EmptyState` (normal), not an error.
- Oversized/invalid-type upload → backend 400 surfaced near the upload control.
- Delete → item removed from the gallery with feedback.

## Open Questions

- None outstanding — shipped.
