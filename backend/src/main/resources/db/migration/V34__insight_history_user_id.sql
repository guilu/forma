-- FOR-145c gap-table migration (5 of 5, ADR-012 design section 3 "5 GAP tables").
--
-- insight_history (FOR-110, migration V10) had ZERO owner-scoping AND the same class of PK bug as
-- V31's training_session_status: its PK is the bare week_start_date, a period key that is
-- IDENTICAL for every user -- two accounts' insights for the same calendar week would collide on
-- one row. Fixing this needs PK reconstruction to a composite (user_id, week_start_date), so this
-- migration reuses the V28/V31 SHADOW-TABLE SWAP pattern for BOTH insight_history (parent) and its
-- child insight_history_recommendation, whose own PK (week_start_date, sort_order) and FK to the
-- parent must be rebuilt in lockstep to include user_id (a plain "scope the child via the parent's
-- new user_id" join is not enough here because the child's own PK also collides across users
-- without it).
--
--   (1) CREATE insight_history_v2 with user_id UUID NOT NULL REFERENCES users(id) and
--       PRIMARY KEY (user_id, week_start_date).
--   (2) CREATE insight_history_recommendation_v2 with user_id UUID NOT NULL, composite
--       PRIMARY KEY (user_id, week_start_date, sort_order), and a composite FK
--       (user_id, week_start_date) -> insight_history_v2(user_id, week_start_date) -- valid
--       because that pair is the parent's primary key (implicitly unique).
--   (3) INSERT ... SELECT into both _v2 tables, backfilling every existing row onto the
--       placeholder UUID -- same defensive rationale as V27/V28/V30/V31: there is only ever one
--       legacy account pre-145a.
--   (4) DROP the child, then the parent (FK order), then RENAME both _v2 tables back. No other
--       table has an FK referencing insight_history -- verified by grepping every migration for
--       "REFERENCES insight_history".
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011); verified by this
-- migration's own migration test.
CREATE TABLE insight_history_v2 (
    user_id                      UUID NOT NULL REFERENCES users (id),
    week_start_date              DATE NOT NULL,
    latest_weight_kg             NUMERIC(6, 3),
    latest_body_fat_percentage   NUMERIC(5, 2),
    latest_lean_mass_kg          NUMERIC(6, 3),
    planned_running_sessions     INTEGER NOT NULL,
    completed_running_sessions   INTEGER NOT NULL,
    planned_strength_sessions    INTEGER NOT NULL,
    completed_strength_sessions  INTEGER NOT NULL,
    notes                        TEXT,
    generated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, week_start_date)
);

INSERT INTO insight_history_v2
    (user_id, week_start_date, latest_weight_kg, latest_body_fat_percentage, latest_lean_mass_kg,
     planned_running_sessions, completed_running_sessions, planned_strength_sessions,
     completed_strength_sessions, notes, generated_at)
SELECT
    '00000000-0000-0000-0000-000000000000',
    week_start_date, latest_weight_kg, latest_body_fat_percentage, latest_lean_mass_kg,
    planned_running_sessions, completed_running_sessions, planned_strength_sessions,
    completed_strength_sessions, notes, generated_at
FROM insight_history;

CREATE TABLE insight_history_recommendation_v2 (
    user_id          UUID NOT NULL,
    week_start_date  DATE NOT NULL,
    sort_order       INTEGER NOT NULL,
    is_main          BOOLEAN NOT NULL,
    category         VARCHAR(16) NOT NULL,
    severity         VARCHAR(16) NOT NULL,
    message          TEXT NOT NULL,
    reason           TEXT NOT NULL,
    related_metric   VARCHAR(64),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, week_start_date, sort_order),
    FOREIGN KEY (user_id, week_start_date)
        REFERENCES insight_history_v2 (user_id, week_start_date)
);

INSERT INTO insight_history_recommendation_v2
    (user_id, week_start_date, sort_order, is_main, category, severity, message, reason,
     related_metric, created_at)
SELECT
    '00000000-0000-0000-0000-000000000000',
    week_start_date, sort_order, is_main, category, severity, message, reason, related_metric,
    created_at
FROM insight_history_recommendation;

DROP TABLE insight_history_recommendation;
DROP TABLE insight_history;
ALTER TABLE insight_history_v2 RENAME TO insight_history;
ALTER TABLE insight_history_recommendation_v2 RENAME TO insight_history_recommendation;
