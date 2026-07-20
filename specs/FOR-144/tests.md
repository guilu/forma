# FOR-144 Test Plan

**SHIPPED** — tests delivered with commit `1ab9d95`. Documented here for completeness.

## Scope

The progress-photo upload, gallery and delete UI consuming FOR-140. Backend out of scope.

## API client (photos)

- `POST /api/v1/progress/photos` — multipart `file`; oversized/invalid type → 400 `VALIDATION_ERROR`
  surfaces as a rejected promise.
- `GET /api/v1/progress/photos` — metadata list.
- `GET /api/v1/progress/photos/{id}` — owner-scoped binary.
- `DELETE /api/v1/progress/photos/{id}` — removes a photo.

## Gallery / upload UI

- Empty gallery → `EmptyState` (normal), not an error.
- Upload a valid image → appears in the gallery.
- Upload oversized/invalid type → error surfaced near the control (from the backend 400).
- Delete → item removed with feedback.
- Loading → `LoadingState`; fetch error → `ErrorState` (FOR-60).

## Privacy

- The photo binary is fetched only via the owner-scoped endpoint (no public URL asserted).

## Accessibility

- Upload control and gallery items labelled; states announced; axe coverage.

## Fixtures

- Mocked photo metadata list + binary, plus an oversized/invalid-type upload error.
