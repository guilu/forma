-- Withings OAuth: encrypted token storage + OAuth state/PKCE challenges (FOR-131, slice 2 of
-- FOR-103, on top of the FOR-126 connection shell). Additive on top of V14 (ADR-003) — earlier
-- migrations are untouched. Two new tables, both scoped by (owner_id, provider) like
-- integration_connection (V12), but deliberately NOT added as columns to that table — it stays
-- token-free by design (spec FOR-126/FOR-131 boundary rule) so the IntegrationRepository port never
-- has to change to accommodate OAuth/token concerns.

-- Encrypted-at-rest provider OAuth tokens (spec FOR-131 Data Model Notes: "AES-256-GCM ciphertext +
-- per-token nonce columns for access + refresh tokens, plus expiry"). Application-level AES-256-GCM
-- (see AesGcmTokenCipher) — plaintext tokens never touch this table. The GCM authentication tag is
-- not a separate column: the JCE AES/GCM/NoPadding cipher appends the 128-bit tag to the ciphertext
-- bytes it returns, so "ciphertext + nonce (and auth tag)" from the spec is satisfied by
-- {ciphertext column already containing the tag} + {nonce column}. One row per (owner_id,
-- provider), upserted like integration_connection (V12) and user_profile (V8) — a connection has at
-- most one live token pair per provider.
CREATE TABLE integration_token (
    owner_id                 VARCHAR(64) NOT NULL,
    provider                 VARCHAR(32) NOT NULL,
    access_token_ciphertext  BYTEA NOT NULL,
    access_token_nonce       BYTEA NOT NULL,
    refresh_token_ciphertext BYTEA NOT NULL,
    refresh_token_nonce      BYTEA NOT NULL,
    access_token_expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (owner_id, provider)
);

-- Short-lived, single-use OAuth state (CSRF nonce) + PKCE (RFC 7636) challenge, issued by
-- IntegrationService#connect and consumed exactly once by IntegrationService#callback (spec
-- FOR-131 Data Model Notes: "a short-lived persisted table... must expire and be single-use").
-- Resolved Open Question: persisted (not in-memory) — FORMA already has Postgres/Flyway for every
-- other piece of state, and a persisted challenge survives an application restart during the
-- realistic multi-second-to-minutes gap between "user clicked connect" and "user finished the
-- Withings consent screen", which an in-memory store would lose.
--
-- Not encrypted: state/code_verifier/code_challenge are CSRF/PKCE material, not provider access or
-- refresh tokens — the spec's encryption-at-rest requirement targets provider tokens specifically
-- (see integration_token above). One row per (owner_id, provider): a fresh connect overwrites any
-- previous unconsumed challenge for the same owner/provider, so re-triggering connect before
-- finishing a prior attempt simply invalidates it rather than accumulating rows. "Single-use" is
-- enforced by the adapter deleting the row on a successful consume, not by a schema constraint.
CREATE TABLE integration_oauth_state (
    owner_id       VARCHAR(64) NOT NULL,
    provider       VARCHAR(32) NOT NULL,
    state          VARCHAR(128) NOT NULL,
    code_verifier  VARCHAR(128) NOT NULL,
    code_challenge VARCHAR(128) NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (owner_id, provider)
);
