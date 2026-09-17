-- Login with Google (ADR-012 addendum). Adds federated login alongside the existing
-- email/password flow: an account can now exist with no password at all (Google-only) or with
-- both a password and a linked Google identity.
--
-- 1) users.password_hash becomes optional.
--
-- V26 declared it NOT NULL because password/email was the only way to create an account. A
-- Google-only account never sets one -- UserService#loginWithGoogle never calls the password
-- encoder for it (ADR-012 rule: never fabricate a password hash for a credential the user never
-- chose). Every password-login code path already treats a null encoded password as "does not
-- match" via Argon2PasswordEncoder/DelegatingPasswordEncoder's own null guards -- not a new
-- runtime branch here.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 2) users.google_subject stores the stable Google "sub" claim (never the email, which a Google
-- account holder could in theory change at the provider) -- the join key back to a FORMA account.
-- Nullable: only accounts that have ever completed a Google login carry one. Unique: two FORMA
-- accounts can never point at the same Google identity, matching the email uniqueness invariant
-- already enforced by idx_users_email.
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);

CREATE UNIQUE INDEX idx_users_google_subject ON users (google_subject);
