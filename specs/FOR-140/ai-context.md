# FOR-140 AI Context

## Story

FOR-140 — Progress photos: private storage + retrieval. Progress-photos slice (slice 6) of
FOR-104 [STUB] Progress & goals domain. **Privacy review required.**

## Intent

Let users store progress photos and retrieve them via an owner-scoped, access-controlled
reference — never a public URL. Success = upload/list/retrieve/delete work, binaries stay
private, content is never logged, and non-owner access is denied. Privacy correctness > speed.

## ⚠️ Privacy is the primary NFR

Progress photos are sensitive personal data (ADR-002, AGENTS.md "never log sensitive health data").
Private storage, access-controlled references, no content in logs, owner-scoped. **A privacy
review is a Definition-of-Done gate before merge.**

## Key finding (verified) — net-new, first binary-storage concern

- **Not implemented today** — no photo domain/adapter/endpoint exists.
- **No object storage in the backend** — all persistence is JDBC/PostgreSQL; no S3/MinIO/blob client, no `MultipartFile` usage yet. This slice introduces the first binary store and the first multipart upload.
- Owner-scoping shape exists (`goal.owner_id`, `earned_achievement.owner_id`) — reuse it for photo metadata.

## Relevant Documents

- `specs/FOR-104/` — full stub scope; slice-6 (privacy) details.
- `AGENTS.md` — hexagonal, owner-scoping, never log sensitive data, no wildcard CORS, no bypassing authorization "because MVP is single-user".
- `docs/adr/ADR-002-authentication.md` — server-side authorization, reject cross-user reads/writes, do not rely on UI filtering, do not log credentials/sensitive data.
- `docs/adr/ADR-003-persistence.md` — migration-driven schema; persistence adapters translate; **the binary-storage decision (object store vs DB blob) must be ADR-003-consistent and documented in design**.
- `docs/adr/ADR-005-api-design.md` — predictable errors, no stack traces, validate at boundaries.
- Jira: https://dbhlab.atlassian.net/browse/FOR-140

## Domain / Repo Notes (verified)

- New: `ProgressPhoto` metadata model; `ProgressPhotoRepository` port (metadata) + `ProgressPhotoStore` port (binary), adapters in `adapter/`.
- Metadata migration — head is **V18**, next free is **V19**; **assign the exact number at implementation** (independent slice; whichever V19-claiming slice merges first wins). One column per statement.
- Delivery: a thin `/progress/photos` controller; storage behind the port; retrieval streams through an owner-scoped, access-controlled endpoint.

## Architectural Constraints

- Hexagonal: storage behind a port/adapter; thin controller; domain/persistence never leaked as API types.
- Binary stays private (out of DB, or a documented ADR-003-consistent DB-blob decision); metadata in DB.
- Owner-scoped (ADR-002); non-owner access denied (403) even single-user.
- **Content never logged** — no bytes, no content-derived fields, no full storage path in any log line.

## Common Pitfalls

- Returning or storing a public/static/durable URL for a photo — retrieval must be access-controlled.
- Logging photo content, filenames-as-content, or storage internals.
- Bypassing the owner boundary "because there's only one user".
- Hard-coding V19 before implementation (number is claimed at merge time).
- Skipping content-type/size validation (untrusted upload).
- Building object-store infra speculatively if a simpler private store satisfies the slice — decide in design, document the tradeoff.

## Suggested Implementation Order

1. `ProgressPhoto` metadata model + `ProgressPhotoRepository`/`ProgressPhotoStore` ports (tested with fakes).
2. Metadata migration (V-number assigned at implementation) + JDBC metadata adapter + private-store adapter per the storage decision.
3. Application service: upload (validate → store binary → write metadata), list, owner-scoped retrieve, delete.
4. Delivery: `POST/GET/GET{id}/DELETE /progress/photos` + DTOs; 403 non-owner, 404 unknown, 400 invalid upload; assert no-content-in-logs.
5. **Privacy review** before merge.

## Validation

Backend build + tests (`./gradlew build`). Confirm: upload stores privately (binary behind the port,
metadata in DB); retrieval is owner-only and access-controlled (no public URL); non-owner → 403;
unknown/deleted → 404; invalid upload → 400; **photo content never appears in logs** (asserted);
migration added for metadata; privacy review completed.
