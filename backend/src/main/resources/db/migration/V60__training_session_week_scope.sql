-- Training: scope session status to its week, and stop naming sessions by their day.
--
-- TWO BUGS, ONE ROOT CAUSE. `training_session_status` (V3, re-keyed by V31) stored one row per
-- (user_id, session_id) where session_id was "MONDAY:RUNNING" -- the day of the week WAS the
-- session's identity, and nothing recorded which week the row belonged to. That single modelling
-- choice produced both of these:
--
--   (1) Status never expired. A session marked COMPLETED in any past week was replayed onto every
--       following week forever, so "Resumen semanal" showed 2/6 on a Monday morning with nothing
--       done yet. There was no weekly reset anywhere -- there was nothing to reset AGAINST.
--   (2) Sessions could not be moved. Rescheduling Monday's easy run to Tuesday would have changed
--       the session's identity rather than its date, because identity and date were the same
--       string.
--
-- The fix separates the three facts that "MONDAY:RUNNING" had fused into one:
--
--   * WHAT the session is  -> session_key   ("RUNNING:EASY", "STRENGTH:PUSH") -- no day in it.
--   * WHEN it is planned   -> scheduled_day (NULL = the day WeeklyTrainingDayPolicy assigns it).
--   * WHEN it was done     -> completed_at.
--
-- session_key is unique within a week by construction: RunningPlanGenerator emits exactly one
-- session per type per week (EASY Monday, INTERVALS-or-RECOVERY Wednesday, LONG_RUN Saturday) and
-- WeeklyTrainingDayPolicy assigns exactly one template per WorkoutType (PUSH/PULL/LEGS).
--
-- EXISTING ROWS ARE DISCARDED, not backfilled. A row's week was never recorded and is not
-- recoverable from anything else in the schema; assigning it a week_start would be fabricating
-- history rather than migrating it. The only cost is that sessions completed before this migration
-- revert to PLANNED -- which is precisely the stale state bug (1) describes.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011); no other table has an
-- FK referencing this one (verified by grepping every migration for
-- "REFERENCES training_session_status"), so the DROP is safe.
DROP TABLE training_session_status;

CREATE TABLE training_session_status (
    user_id       UUID NOT NULL REFERENCES users (id),
    week_start    DATE NOT NULL,
    session_key   VARCHAR(64) NOT NULL,
    scheduled_day VARCHAR(9),
    status        VARCHAR(16) NOT NULL,
    completed_at  TIMESTAMP,
    notes         TEXT,
    PRIMARY KEY (user_id, week_start, session_key)
);
