# FOR-140 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-140
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (progress-photos slice, slice 6).

## ⚠️ PRIVACY-SENSITIVE — privacy review REQUIRED before merge

Progress photos are sensitive personal data. This slice is about doing storage + retrieval
**correctly and privately**, not quickly. Privacy is the primary NFR (ADR-002, AGENTS.md
"never log sensitive health data"). A privacy review is a Definition-of-Done gate.

## Summary

Store progress photos in a **private store behind a port/adapter** (binary out of the DB, or a
documented DB-blob decision), with metadata in the DB, and retrieve them only via an
**owner-scoped, access-controlled reference — never a public/static URL**. Photo content is
**never written to logs**. Non-owner access is **denied even in the single-user MVP**.

## Repository baseline (verified)

- **Not implemented today** — no photo domain/adapter/endpoint exists (searched).
- **No object storage exists yet** — no S3/MinIO/blob client in the backend; all persistence today is JDBC/PostgreSQL (`adapter/persistence/Jdbc*Repository`). This slice introduces the first binary-storage concern and the first multipart upload (`MultipartFile`) in the codebase.
- Owner-scoping shape convention exists (`goal.owner_id`, `meal_log_entry.owner_id`, `earned_achievement.owner_id`) — reuse it for photo metadata.
- API surface: add a new `delivery/progress/photos` controller (or extend the `/progress` area) — keep it thin; storage lives behind a port.

## Storage decision (flag for THIS slice's design; ADR-003-consistent)

Decide binary storage in design and **document the tradeoffs**:

- **Option A — private object store** (e.g. filesystem-backed private dir now, S3/MinIO-compatible later) behind a `ProgressPhotoStore` port. Keeps large binaries out of Postgres; retrieval streams through the access-controlled endpoint. Preferred if a private store is available.
- **Option B — DB blob** (`BYTEA`) in a metadata+content table. Simplest, transactional, no new infra; heavier DB and streaming cost. Acceptable for MVP volume if documented ADR-003-consistent.

Either way: **metadata in the DB; binary behind the port**; **no public/static hosting**; the
GET returns bytes through an owner-scoped, access-controlled endpoint (optionally a short-lived,
owner-scoped reference token) — **never a durable public URL**.

## Migration (metadata) — next free number, claimed at implementation

- Photo **metadata** needs a new migration. Head is **V18**; the next free number is **V19** —
  but FOR-140 and FOR-137's stub both once proposed "V19+". FOR-137 is already at `V11` (no new
  migration), so in practice **only FOR-140 needs a new migration**. Still, the exact V-number is
  **assigned at implementation time** (whichever independent slice merges first takes V19); do not
  hard-code V19 now. One column per statement (ADR-003).
- Metadata columns (indicative): `id UUID PK`, `owner_id VARCHAR(64) NOT NULL`, `content_type`,
  `size_bytes`, `created_at TIMESTAMP WITH TIME ZONE`, a `storage_ref` (opaque key into the private
  store) OR the blob column if Option B. `owner_id`-indexed. **No public URL column.**

## User/System Flow

1. User uploads a progress photo (multipart) → `POST /api/v1/progress/photos` → binary stored privately, metadata row written, private reference id returned.
2. User lists photos → `GET /api/v1/progress/photos` → metadata only (ids, timestamps, content-type) — no binary, no URL.
3. User views a photo → `GET /api/v1/progress/photos/{id}` → owner-scoped, access-controlled binary stream. Non-owner → 403.
4. User deletes a photo → `DELETE /api/v1/progress/photos/{id}` → metadata + binary removed.

## Functional Requirements

- Upload: accept a validated image (content-type + size limits); store binary via the port; write metadata; return a private reference id.
- List: metadata only, owner-scoped.
- Retrieve: owner-scoped, access-controlled binary; never a public/static URL.
- Delete: owner-scoped removal of metadata + binary.
- Non-owner access to any photo → denied (403), even in single-user MVP — do NOT bypass the boundary.

## Non-Functional Requirements

- **Privacy (primary)**: private storage; access-controlled references; **content never logged** (no bytes, no filename-derived content, no full path in logs); owner-scoped (ADR-002). Privacy review is a DoD gate.
- **Persistence**: metadata migration-driven (ADR-003); binary behind a port (hexagonal).
- **Security**: enforce content-type/size limits; reject non-image uploads; predictable error shapes (ADR-005), no stack traces to clients.

## Data Model Notes

- New domain/application: a `ProgressPhoto` metadata model + a `ProgressPhotoStore` port (binary) and a `ProgressPhotoRepository` port (metadata). Adapter(s) in `adapter/`.
- Metadata in DB (new migration); binary behind the port per the storage decision.
- `storage_ref` is opaque and internal — never surfaced as a client-resolvable URL.

## Edge Cases

- Access by anything other than the owner → 403 (boundary enforced even single-user).
- Unknown photo id → 404.
- Delete then GET → 404.
- Oversized / non-image upload → 400 `VALIDATION_ERROR`.
- Empty list (no photos) → 200 empty, never 404.
- Log assertions: photo bytes / content never appear in any log line.

## Open Questions

- Storage: private object store vs DB blob — decide in design, ADR-003-consistent (default to the simplest that keeps binaries private; if no private store exists yet, a filesystem-backed private dir or `BYTEA` blob are the MVP candidates).
- Reference style for retrieval: direct owner-scoped streaming endpoint vs short-lived owner-scoped token — pick + document; both must avoid a durable public URL.
- Content-type allow-list and max size for MVP.
- Exact migration V-number (assigned at implementation; V19 if FOR-140 merges before any other V19-claiming slice).
