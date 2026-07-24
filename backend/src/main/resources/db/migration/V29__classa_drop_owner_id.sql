-- Class-A owner_id contract migration (FOR-145b-2, ADR-012 design section 3, phase 3 of
-- expand/migrate/contract).
--
-- V27 (145b-1) added the real user_id UUID column to the 5 "Class A" tables (owner_id is a plain
-- non-PK column: goal, meal_log_entry, water_intake_entry, progress_photo,
-- weekly_tracking_record) and switched every read/write in the application layer to user_id,
-- while keeping the legacy owner_id VARCHAR column alive (write-only, populated with
-- userId.toString(), never read) so existing rows kept their original shape during the
-- verification window.
--
-- Now that user_id has been the sole source of truth in the app layer for a full release (145b-1
-- merged, 145b-2 wired the remaining Class-B domains), this is the CONTRACT phase: drop the
-- legacy owner_id column and its now-redundant index from all 5 tables. This mirrors V28's
-- contract of the equivalent legacy column on the 6 Class-B tables (already fully dropped there,
-- as part of the shadow-table swap).
--
-- Each table's own legacy owner_id index (created by V11/V13/V14/V19/V21) is dropped explicitly
-- before the column, for portability -- Postgres implicitly drops indexes owned solely by a
-- dropped column, but H2 (MODE=PostgreSQL) is not guaranteed to behave identically, so this
-- migration never relies on implicit cascade behavior (ADR-003 plain-SQL, no engine-specific
-- shortcuts). The user_id index/unique-index added by V27 is untouched -- it is the only index
-- these tables need going forward.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).

-- ---------------------------------------------------------------------------------------------
-- goal (V11 owner_id, V27 user_id)
DROP INDEX idx_goal_owner;
ALTER TABLE goal DROP COLUMN owner_id;

-- ---------------------------------------------------------------------------------------------
-- meal_log_entry (V13 owner_id, V27 user_id)
DROP INDEX idx_meal_log_entry_owner_date;
ALTER TABLE meal_log_entry DROP COLUMN owner_id;

-- ---------------------------------------------------------------------------------------------
-- water_intake_entry (V14 owner_id, V27 user_id)
DROP INDEX idx_water_intake_entry_owner_date;
ALTER TABLE water_intake_entry DROP COLUMN owner_id;

-- ---------------------------------------------------------------------------------------------
-- progress_photo (V19 owner_id, V27 user_id)
DROP INDEX idx_progress_photo_owner;
ALTER TABLE progress_photo DROP COLUMN owner_id;

-- ---------------------------------------------------------------------------------------------
-- weekly_tracking_record (V21 owner_id, V27 user_id)
DROP INDEX idx_weekly_tracking_record_owner_week;
ALTER TABLE weekly_tracking_record DROP COLUMN owner_id;
