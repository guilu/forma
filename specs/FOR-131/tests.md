# FOR-131 Test Plan

Security-sensitive. Strict TDD: failing tests first at each layer. Token/secret-leak assertions are first-class, not optional. Never call the live Withings API in tests — use recorded fixtures / mocked HTTP.

## Scope

Withings OAuth connect/disconnect + encrypted token storage. NO real measures sync (FOR-103 slice 3). Tokens must never leak.

## Adapter Tests (recorded fixtures / mocked HTTP)

- Authorization URL is built with client id, redirect URI, scope, state, PKCE challenge (assert query params; never a secret in a log).
- Token exchange: given a recorded Withings token response, parse access/refresh/expiry.
- Token refresh: given a recorded refresh response, produce new tokens.
- Token store round-trip is **encrypted at rest** — the stored bytes are NOT the plaintext token (assert).
- Revoke/forget clears the stored tokens.

## Application Tests

- `connect` produces an authorization URL and persists a single-use, expiring state challenge; the application port exposes no token accessor (compile-time + review).
- Callback with a valid state → tokens exchanged (via the mocked adapter), stored encrypted, connection CONNECTED.
- Callback with mismatched/expired/replayed state → rejected, no connection, no tokens stored.
- Token exchange failure → connection not CONNECTED, readable outcome, no secret leak.
- Refresh failure → connection marked needing re-auth, no silent drop.
- Disconnect → tokens removed; none remain at rest.

## API Tests

- `POST /{provider}/connect` → 200 with `authorizationUrl`; unknown provider → 400.
- `GET /{provider}/callback` valid → CONNECTED (or documented redirect); mismatched/expired state → 400, no connection.
- `DELETE /{provider}` → DISCONNECTED and no tokens at rest afterwards.
- **No response body, header, log line, or error message contains a token, `code`, or `state`** (assert explicitly across connect/callback/disconnect).

## Edge Cases

- Callback replay (same code/state twice) → rejected/idempotent, no double connection.
- Connect when already CONNECTED → documented behavior.
- Withings unreachable during exchange → readable failure, no secret leak.

## Fixtures

- Recorded Withings authorize + token + refresh responses as local fixtures — never the live API.
- A test encryption key from config, never a committed real secret.
- H2-in-PostgreSQL-mode with Flyway (V15) for the encrypted token store + state challenge persistence tests.
