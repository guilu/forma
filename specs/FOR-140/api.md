# FOR-140 API Spec

> Progress-photos subset of `specs/FOR-104/api.md`, scoped to this slice. Aligns with ADR-005.
> **Privacy-sensitive**: retrieval is owner-scoped and access-controlled — **never a public URL**.
> Confirm exact paths against `ApiPaths.java` and the progress delivery package.

## Endpoints

### POST /api/v1/progress/photos
Upload a progress photo (`multipart/form-data`). Returns a **private reference id** (metadata),
never a public URL. → 201.

### GET /api/v1/progress/photos
List the owner's photos — **metadata only** (id, content-type, size, createdAt). No binary, no URL.

### GET /api/v1/progress/photos/{id}
Owner-scoped, access-controlled **binary** retrieval (streamed). Non-owner → 403.

### DELETE /api/v1/progress/photos/{id}
Owner-scoped delete of metadata + binary. → 204.

## Request

`POST /api/v1/progress/photos` — `multipart/form-data` with an image part (e.g. `file`).
- Enforce a content-type allow-list (e.g. `image/jpeg`, `image/png`) and a max size.
- **Photo content is never logged** (no bytes, no content-derived data).

## Response

`POST /api/v1/progress/photos`
```json
{ "id": "…", "contentType": "image/jpeg", "sizeBytes": 20480, "createdAt": "2026-07-18T10:00:00Z" }
```

`GET /api/v1/progress/photos`
```json
{ "photos": [ { "id": "…", "contentType": "image/jpeg", "sizeBytes": 20480, "createdAt": "2026-07-18T10:00:00Z" } ] }
```
- No field in any response is a public/static/durable URL. Retrieval always goes through the access-controlled `GET /photos/{id}`.

`GET /api/v1/progress/photos/{id}` → the raw image bytes with the stored `Content-Type` (streamed), only to the owner.

## Errors

- 400 `VALIDATION_ERROR` — non-image content-type, oversized upload, malformed multipart.
- 403 Forbidden — accessing another owner's photo (do NOT bypass, even single-user MVP).
- 404 Not Found — unknown photo id; deleted-then-fetched.
- Empty list (no photos) → 200 empty, never 404.
- No stack traces to clients (ADR-005); errors never echo photo content.

## Authorization

Single-user MVP (ADR-002), **strictly owner-only** for photos. Retrieval is access-controlled
(direct owner-scoped streaming endpoint, or a short-lived owner-scoped reference — document the
choice). **Never** a public/static/durable URL. Cross-owner read/write/delete → 403.

## Validation

- Upload content-type ∈ allow-list; size ≤ max; else 400.
- `{id}` must resolve to an owner-owned photo → else 404 (unknown) or 403 (other owner).
- **Content never logged** — assert in tests.
