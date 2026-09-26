-- Training: per-set log (training-progression-and-logging slice B, design D6/D7).
--
-- Persists weight, reps and completion per set, keyed by (user, week, session, exercise, set
-- number) so the log survives a reload and stays scoped to the week it was recorded in, mirroring
-- how V60 scopes `training_session_status` to `(user_id, week_start, session_key)`.
--
-- No FK to the in-code exercise catalog (see JdbcExerciseCatalogRepository/WorkoutTemplateCatalog,
-- V24): templates live in code, not in rows, so an FK here would not guarantee what matters, and a
-- catalog cleanup would either be blocked by real history or silently orphan it. Same precedent as
-- session_key on training_session_status, which also has no FK.
CREATE TABLE training_set_log (
    user_id     UUID NOT NULL REFERENCES users (id),
    week_start  DATE NOT NULL,
    session_key VARCHAR(64) NOT NULL,
    exercise_id VARCHAR(64) NOT NULL,
    set_number  INTEGER NOT NULL,
    weight_kg   NUMERIC(6,2),
    reps        INTEGER,
    done        BOOLEAN NOT NULL DEFAULT FALSE,
    logged_at   TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, week_start, session_key, exercise_id, set_number),
    CONSTRAINT ck_tsl_set_number CHECK (set_number >= 1),
    CONSTRAINT ck_tsl_weight     CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT ck_tsl_reps       CHECK (reps IS NULL OR reps >= 0)
);
