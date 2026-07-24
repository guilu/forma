-- Class-A owner_id(VARCHAR) -> user_id(UUID) migration (FOR-145b-1, ADR-012 design section 3).
--
-- "Class A" = owner_id is a PLAIN non-PK column (expand/migrate, no PK reconstruction needed):
-- goal (V11), meal_log_entry (V13), water_intake_entry (V14), progress_photo (V19),
-- weekly_tracking_record (V21). "Class B" (owner_id inside the PK: user_profile,
-- integration_connection/token/oauth_state/measure_marker, earned_achievement) is a LATER
-- migration (145b-2, V28/V29) that needs DROP/ADD PRIMARY KEY -- not touched here.
--
-- Pattern per table (one ADD COLUMN per ALTER, ADR-003/011 convention):
--   (1) ADD COLUMN user_id UUID (nullable)
--   (2) UPDATE ... SET user_id = PLACEHOLDER WHERE owner_id = 'default-user'  (legacy backfill)
--   (3) defensive idempotent NULL-repair: any row NOT matching the legacy default-user owner (e.g.
--       already non-default, or somehow still NULL) also maps to the placeholder -- mirrors V25's
--       "defensive idempotent NULL-repair" precedent (never orphan a row, never fail the migration;
--       there is only ever one legacy account pre-145a so every existing row's true owner IS the
--       placeholder).
--   (4) ALTER COLUMN user_id SET NOT NULL
--   (5) ADD CONSTRAINT FK user_id -> users(id)
--   (6) add a new user_id index/unique-index alongside the existing owner_id one (owner_id stays
--       queryable/indexed too -- it is NOT dropped here; the legacy column is kept alive until the
--       app-layer switch (this same PR) is verified and a later contract migration drops it, per
--       the design's expand/migrate/contract phasing).
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).

-- ---------------------------------------------------------------------------------------------
-- goal (V11)
ALTER TABLE goal ADD COLUMN user_id UUID;
UPDATE goal SET user_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id = 'default-user';
UPDATE goal SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE goal ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE goal ADD CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_goal_user ON goal (user_id);

-- ---------------------------------------------------------------------------------------------
-- meal_log_entry (V13)
ALTER TABLE meal_log_entry ADD COLUMN user_id UUID;
UPDATE meal_log_entry SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE owner_id = 'default-user';
UPDATE meal_log_entry SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE meal_log_entry ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE meal_log_entry
  ADD CONSTRAINT fk_meal_log_entry_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_meal_log_entry_user_date ON meal_log_entry (user_id, log_date);

-- ---------------------------------------------------------------------------------------------
-- water_intake_entry (V14)
ALTER TABLE water_intake_entry ADD COLUMN user_id UUID;
UPDATE water_intake_entry SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE owner_id = 'default-user';
UPDATE water_intake_entry SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;
ALTER TABLE water_intake_entry ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE water_intake_entry
  ADD CONSTRAINT fk_water_intake_entry_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_water_intake_entry_user_date ON water_intake_entry (user_id, log_date);

-- ---------------------------------------------------------------------------------------------
-- progress_photo (V19)
ALTER TABLE progress_photo ADD COLUMN user_id UUID;
UPDATE progress_photo SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE owner_id = 'default-user';
UPDATE progress_photo SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE progress_photo ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE progress_photo
  ADD CONSTRAINT fk_progress_photo_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_progress_photo_user ON progress_photo (user_id);

-- ---------------------------------------------------------------------------------------------
-- weekly_tracking_record (V21) -- unique (owner_id, week) is rebuilt as unique (user_id, week);
-- the legacy unique index on owner_id stays in place alongside it (owner_id column not dropped).
ALTER TABLE weekly_tracking_record ADD COLUMN user_id UUID;
UPDATE weekly_tracking_record SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE owner_id = 'default-user';
UPDATE weekly_tracking_record SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;
ALTER TABLE weekly_tracking_record ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE weekly_tracking_record
  ADD CONSTRAINT fk_weekly_tracking_record_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE UNIQUE INDEX idx_weekly_tracking_record_user_week ON weekly_tracking_record (user_id, week);
