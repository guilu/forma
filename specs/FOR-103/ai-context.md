# FOR-103 AI Context

## Story

FOR-103 — [STUB] Integrations backend (Withings OAuth + sync). Epic-sized and **security-sensitive**; split into the *Proposed story slices* in `spec.md` and implement one per PR, each with a security review before merge.

## Intent

Replace the FOR-57 mock / FOR-51 static widget / hardcoded sidebar status with a real integrations backend: connect/disconnect providers, store tokens encrypted, sync provider data into the normalized `BodyMeasurement` domain, and expose connection + sync status. Success = the integrations UI reflects real state and Withings body measures flow into FORMA.

## Relevant Documents

- `AGENTS.md` — forbidden shortcuts (never commit/log tokens; never expose provider tokens to domain/frontend), hexagonal boundaries.
- `docs/adr/ADR-004-integrations.md` — adapters behind provider-neutral ports; normalize at the boundary; idempotent sync; mandatory duplicate detection; respect rate limits; failures observable without leaking secrets.
- `docs/adr/ADR-002-authentication.md` — single-user MVP owner scoping.
- `docs/adr/ADR-003-persistence.md`, `ADR-005-api-design.md`, `ADR-008-observability.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-103

## Domain Notes

- **No integrations code exists today** — this story introduces it.
- Sync target already exists: `BodyMeasurement` aggregate + `MeasurementSource` enum. Verify whether a `WITHINGS` source value exists; extend if absent.
- Provider payloads must be mapped to normalized measures at the adapter boundary — never stored as the primary domain model (ADR-004).
- Tokens are an adapter/persistence concern only; the application port must not expose them.

## Architectural Constraints

- Hexagonal: provider-neutral application ports; Withings-specific logic isolated in an adapter.
- Encrypted token storage; OAuth state/PKCE for CSRF protection; secrets from config/env only.
- Idempotent sync + duplicate detection (ADR-004).
- Owner-scoped (ADR-002); never bypass authorization because MVP is single-user.
- New migration is **V11 or later** (head V10); one column per statement.
- Never log tokens, OAuth `code`/`state`, or measure values.

## Common Pitfalls

- Leaking a token into a response, log line, error message, or the domain — assert against this in tests.
- Letting Withings payload shapes leak into the core domain instead of normalizing.
- Non-idempotent sync creating duplicate `BodyMeasurement`s.
- Committing OAuth client credentials or a real encryption key.
- Bypassing the owner boundary "because there's only one user".

## Suggested Implementation Order

1. Provider-neutral connection domain + status read model + API (slice 1) — real contract, still mockable sync.
2. Withings OAuth connect/disconnect + encrypted token storage (slice 2) — **security review**.
3. Withings sync → `BodyMeasurement`, idempotent + duplicate detection (slice 3).
4. Manual sync + last-sync status/outcome (slice 4).
5. (Later) Google Fit / Apple Health behind the same ports.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: status contract works with zero connections; OAuth state validation rejects tampering; tokens are encrypted at rest and never appear in any response/log; sync is idempotent with duplicate detection; disconnect leaves no tokens at rest. A live end-to-end against Withings is out of scope for automated tests — use recorded fixtures.
