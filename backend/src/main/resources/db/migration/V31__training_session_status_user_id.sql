-- FOR-145c gap-table migration (2 of 5, ADR-012 design section 3 "5 GAP tables").
--
-- training_session_status (FOR-27, migration V3) had ZERO owner-scoping AND a genuine
-- correctness bug: its PK is the bare session_id (e.g. "SATURDAY:RUNNING"), a day-of-week-keyed
-- id that is IDENTICAL for every user -- two different accounts both marking "SATURDAY:RUNNING"
-- complete would silently collide on the same row (last writer wins, cross-account data
-- corruption). Fixing this requires PK reconstruction to a composite (user_id, session_id), which
-- H2 cannot do via a bare in-place PK retype (see V28's header comment for the full rationale),
-- so this migration reuses the V28 SHADOW-TABLE SWAP pattern instead of V27/V30's simple ADD
-- COLUMN:
--   (1) CREATE training_session_status_v2 with user_id UUID NOT NULL REFERENCES users(id) and
--       PRIMARY KEY (user_id, session_id).
--   (2) INSERT ... SELECT, backfilling every existing row onto the placeholder UUID -- same
--       defensive rationale as V27/V28/V30: there is only ever one legacy account pre-145a, so
--       every existing row's true owner IS the placeholder.
--   (3) DROP TABLE training_session_status (safe: no other table has an FK referencing it --
--       verified by grepping every migration for "REFERENCES training_session_status").
--   (4) ALTER TABLE training_session_status_v2 RENAME TO training_session_status.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011); verified by this
-- migration's own migration test.
CREATE TABLE training_session_status_v2 (
    user_id    UUID NOT NULL REFERENCES users (id),
    session_id VARCHAR(64) NOT NULL,
    status     VARCHAR(16) NOT NULL,
    notes      TEXT,
    PRIMARY KEY (user_id, session_id)
);

INSERT INTO training_session_status_v2 (user_id, session_id, status, notes)
SELECT '00000000-0000-0000-0000-000000000000', session_id, status, notes
FROM training_session_status;

DROP TABLE training_session_status;
ALTER TABLE training_session_status_v2 RENAME TO training_session_status;
