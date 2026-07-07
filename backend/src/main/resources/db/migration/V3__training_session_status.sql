-- Training: session completion status (FOR-27).
--
-- The weekly training calendar (FOR-26) is composed deterministically, so each
-- session has a stable id (e.g. "SATURDAY:RUNNING"). This table stores the
-- user's completion status (and optional notes) per session id as an override;
-- sessions without a row default to PLANNED. Additive migration on top of V2
-- (ADR-003) — earlier migrations are untouched.
CREATE TABLE training_session_status (
    session_id VARCHAR(64) PRIMARY KEY,
    status     VARCHAR(16) NOT NULL,
    notes      TEXT
);
