# FOR-140 Test Plan

Strict TDD: failing tests first at each layer (metadata/store ports → application service → API),
then implement. **Privacy assertions are first-class**, not an afterthought.

## Scope

Progress-photo private storage + owner-scoped, access-controlled retrieval. New metadata migration;
binary behind a port. Privacy is the primary property under test.

## Domain / Application Tests

- Upload: validated image → binary stored via `ProgressPhotoStore`, metadata row written, private reference id returned.
- Owner retrieval: the owner gets the exact bytes back (round-trip through the store port).
- **Non-owner denied**: retrieval/delete of a photo owned by another owner → denied (403 at the API; boundary enforced in the service), even single-user.
- List: metadata only, owner-scoped; no binary, no URL in the model.
- Delete: metadata + binary removed; subsequent retrieval → not found.
- Validation: non-image content-type / oversized upload → rejected.

## API Tests

- `POST /progress/photos` (multipart) → 201 + private reference (no public URL).
- `GET /progress/photos` empty → 200 empty; after upload → metadata list (no binary/URL).
- `GET /progress/photos/{id}` → binary only to the owner; **non-owner → 403**.
- `DELETE /progress/photos/{id}` → 204; then `GET` → 404.
- Unknown id → 404; non-image/oversized upload → 400 `VALIDATION_ERROR`.
- **No-content-in-logs**: capture logs across upload/retrieve and assert photo bytes / content never appear (first-class privacy test).
- No response field is a public/static/durable URL (assert).

## Edge Cases

- Access as non-owner → 403 (do not bypass even single-user).
- Delete then get → 404.
- Empty list → 200 empty, never 404.
- Oversized / wrong content-type → 400.

## Fixtures

- A small test image for upload/retrieval; assert the stored reference is private (no public URL).
- Two owners (or an injected non-owner principal) for the cross-owner 403 path.
- A log capture (appender/handler) for the no-content-in-logs assertion.
- H2-in-PostgreSQL-mode with Flyway including the new photo-metadata migration (V-number assigned at implementation) for persistence tests; a private-store fake/temp-dir adapter for the binary path.

## Privacy review (DoD gate)

Beyond automated tests, a manual **privacy review** is required before merge: confirm no public
URL, no content in logs, owner-only access, and an ADR-003-consistent storage decision documented
in the design.
