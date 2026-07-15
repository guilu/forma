# FOR-103 Test Plan

Strict TDD: failing tests first at each layer. Security assertions are first-class here, not optional.

## Scope

Provider-neutral connection status, Withings OAuth connect/disconnect, encrypted token storage, idempotent sync into `BodyMeasurement`, status/outcome reporting. Tokens must never leak.

## Domain / Application Tests

- Connection state transitions: DISCONNECTED → connecting → CONNECTED → DISCONNECTED.
- Sync normalizes provider measures into `BodyMeasurement` with `MeasurementSource = WITHINGS`.
- Idempotency: re-running a sync over already-imported measures creates no duplicates (duplicate detection key).
- Unmodeled measure type → skipped, not forced into the domain.
- Token refresh failure → connection marked needing re-auth; no data dropped silently.
- Domain/application services never receive a raw token (port exposes no token accessor).

## Adapter Tests

- Withings payload → normalized measure mapping (fixtures, no live network).
- Token store round-trip is **encrypted at rest** (stored bytes are not the plaintext token).
- Rate-limit / 429 handling backs off and records a readable outcome.

## API Tests

- `GET /integrations` before any connection → 200, all providers DISCONNECTED, never 404.
- `POST /{provider}/connect` unknown provider → 400.
- `GET /{provider}/callback` with mismatched/expired `state` → 400, no connection created.
- `POST /{provider}/sync` → returns imported/duplicate counts; second call imports 0 new.
- `DELETE /{provider}` → status becomes DISCONNECTED; no tokens remain at rest afterwards.
- **No response body, log line, or error message contains a token, `code`, or `state` secret** (assert explicitly).

## Edge Cases

- Disconnect during in-flight sync → safe, no orphaned tokens.
- Provider unreachable → sync outcome is a readable failure, no secret leak.
- Callback replay (same code twice) → rejected/ignored, idempotent.

## Fixtures

- Recorded Withings API responses (auth + measures) as local fixtures — never call the live API in tests.
- A `BodyMeasurement` set to exercise duplicate detection.
- Test encryption key from config, never a committed real secret.
