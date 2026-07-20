# FOR-144 UI Spec

**SHIPPED** in commit `1ab9d95`. Documents the delivered UI.

## Screens

- Progreso → `ProgressPhotosSection` (`frontend/src/pages/progress/ProgressPhotosSection.tsx`, within
  `ProgressPage`; mockup `docs/6-progreso.png`).

## Components

- `ProgressPhotosSection` — upload control + gallery grid + per-photo delete.
- FOR-60 `LoadingState` / `EmptyState` / `ErrorState`; `Card` (`headingLevel`, FOR-112);
  `useNotify` (FOR-63) for feedback.

## States

- **Empty**: no photos → `EmptyState` inviting the first upload (normal state).
- **Loading**: metadata fetch in flight (`LoadingState`).
- **Success**: gallery grid; each image loaded via the owner-scoped `GET /photos/{id}`.
- **Uploading**: pending state on the upload control; validation error (400) surfaced near it.
- **Error**: fetch failed → `ErrorState`.

## Interactions

- Upload → multipart `file` (accept `image/jpeg,image/png`) → photo appears on success.
- Delete a photo → confirmation/feedback → removed from the gallery.

## Accessibility

- Upload control labelled; gallery images have accessible labels; states announced (FOR-60).
- Destructive delete is keyboard-operable with feedback.

## Responsive Behavior

- Gallery grid reflows to fewer columns on mobile (single-column where needed), matching the Progreso pattern.

## Privacy note

- Photo binaries are privacy-sensitive personal data — fetched only through the owner-scoped,
  access-controlled endpoint, never a public URL, never cached in a shared context.
