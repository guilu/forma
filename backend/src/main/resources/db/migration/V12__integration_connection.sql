-- Integration connections (FOR-126, first implementable slice of FOR-103).
--
-- NO tokens/OAuth this slice (spec FOR-126 Data Model Notes) — this table intentionally carries no
-- token/secret column so the application port and this schema stay stable when FOR-103 slices 2-3
-- add encrypted token storage inside the persistence adapter (ADR-004). Additive on top of V11
-- (ADR-003) — earlier migrations are untouched.
--
-- One row per (owner_id, provider), upserted like user_profile (V8, single-row-per-owner shape)
-- rather than goal's list-of-generated-ids shape (V11): a connection's identity IS the
-- (owner, provider) pair — there is never more than one connection per provider per owner, and
-- every endpoint (/integrations/{provider}/...) always addresses it by that pair, never by a
-- separate generated id.
--
-- last_sync_* columns are a small embedded SyncOutcome value flattened onto the row (spec FOR-126
-- Data Model Notes: "a small embedded value or columns on the connection row"), not a separate
-- table — there is at most one "last" outcome per connection, so a child table would only ever
-- hold zero or one row.
--
-- owner_id exists even though authorization is not enforced yet (ADR-002 single-user MVP),
-- mirroring user_profile.owner_id: every row is account-scoped in shape, ready for a real
-- account id once authentication lands.
CREATE TABLE integration_connection (
    owner_id                 VARCHAR(64) NOT NULL,
    provider                 VARCHAR(32) NOT NULL,
    status                   VARCHAR(16) NOT NULL DEFAULT 'DISCONNECTED',
    connected_at             TIMESTAMP WITH TIME ZONE,
    last_sync_at             TIMESTAMP WITH TIME ZONE,
    last_sync_result         VARCHAR(16),
    last_sync_imported_count INTEGER,
    last_sync_message        VARCHAR(500),
    PRIMARY KEY (owner_id, provider)
);
