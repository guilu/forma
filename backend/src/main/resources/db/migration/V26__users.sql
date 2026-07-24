-- Real authentication: users table (FOR-145a, ADR-012).
--
-- First-ever `users` table. Fulfils ADR-011's binding assumption that FOR-145 delivers
-- `users.id UUID PRIMARY KEY` and backfills its `PLACEHOLDER_USER_ID` sentinel
-- ('00000000-0000-0000-0000-000000000000'), which every v2 table (plan, training_plan, ...)
-- already stores in place of a real FK (no FK is added here -- that is a later slice once the
-- legacy `owner_id VARCHAR` tables and the v2 tables are both ready to reference this table).
--
-- Portable on both H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011 conventions):
-- UUID PK, TIMESTAMP WITH TIME ZONE, CREATE UNIQUE INDEX for the email uniqueness invariant
-- (no inline UNIQUE column constraint, matching the V21/V25 precedent).
--
-- The placeholder row is the legacy single-user data owner: it satisfies BOTH the legacy
-- `owner_id = 'default-user'` backfill target (145b/145c) and the ADR-011 v2 `PLACEHOLDER_USER_ID`
-- consumer, so both migration trees can converge on one identical UUID. It is seeded UNUSABLE:
-- `password_hash = '!'` is never a valid Argon2/BCrypt encoding (no algorithm id prefix), and
-- `is_active = FALSE`, so nobody can authenticate as it by any code path until
-- `LegacyUserBootstrap` activates it at runtime from a config-driven property (never a secret
-- committed to this migration -- AGENTS.md "Do not commit secrets").
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_users_email ON users (email);

INSERT INTO users (id, email, password_hash, is_active)
VALUES ('00000000-0000-0000-0000-000000000000', 'legacy@forma.local', '!', FALSE);
