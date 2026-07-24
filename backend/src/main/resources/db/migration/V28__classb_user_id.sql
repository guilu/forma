-- Class-B owner_id(VARCHAR) -> user_id(UUID) migration (FOR-145b-2, ADR-012 design section 3).
--
-- "Class B" = owner_id is the PRIMARY KEY or part of a composite PRIMARY KEY: user_profile (V8,
-- owner_id IS the PK), integration_connection (V12/V16, PK part), integration_token (V15, PK
-- part), integration_oauth_state (V15, PK part), integration_measure_marker (V16, PK part),
-- earned_achievement (V18, PK part). Unlike Class A (V27, a plain non-PK column), a PK column
-- cannot simply be retyped in place: dropping it requires first dropping the PK constraint that
-- references it.
--
-- SHADOW-TABLE SWAP (verified against H2 MODE=PostgreSQL, this migration's own repo/migration
-- test): tried and REJECTED the "ADD user_id, ALTER TABLE ... DROP PRIMARY KEY, ADD PRIMARY KEY"
-- approach from the design doc's sketch -- H2 accepts the MySQL-style bare `DROP PRIMARY KEY`
-- statement even in MODE=PostgreSQL, but real PostgreSQL has no such shorthand (only
-- `DROP CONSTRAINT <name>`), and the unnamed inline `PRIMARY KEY (...)` on these tables (V8/V12/
-- V15/V16/V18) is auto-named DIFFERENTLY by each engine (H2: an opaque `CONSTRAINT_n`; PostgreSQL:
-- the `<table>_pkey` convention) -- so no single constraint name is portable across both without
-- a Java-based migration querying JDBC metadata at runtime (a larger convention change than this
-- migration warrants, see ADR-003's plain-SQL/no-code-migration precedent).
--
-- Instead, each table is rebuilt via a portable, engine-agnostic sequence that never needs to name
-- or discover the old PK constraint:
--   (1) CREATE TABLE <table>_v2 with the exact same columns, EXCEPT owner_id VARCHAR(64) is
--       replaced by user_id UUID (same position in the PK), with an inline FK to users(id).
--   (2) INSERT INTO <table>_v2 SELECT ... FROM <table>, mapping every existing row's owner to the
--       placeholder UUID unconditionally -- same defensive rationale as V27: there is only ever
--       one legacy account pre-145a, so every existing row's true owner IS the placeholder,
--       regardless of what its legacy owner_id string happened to say.
--   (3) DROP TABLE <table> (safe: no other table has an FK referencing any of these six tables --
--       verified by grepping every migration for "REFERENCES <table>").
--   (4) ALTER TABLE <table>_v2 RENAME TO <table>.
-- All four steps are standard ANSI/PostgreSQL SQL, confirmed to also run unchanged on H2
-- MODE=PostgreSQL. Safe at this MVP's current scale (user_profile is one row per owner;
-- integration_*/earned_achievement are a handful of rows per owner) -- a full-table rewrite is not
-- a concern here the way it would be for a large table.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).

-- ---------------------------------------------------------------------------------------------
-- user_profile (V8 + V20 personal-targets columns) -- owner_id WAS the sole PK.
CREATE TABLE user_profile_v2 (
    user_id                              UUID PRIMARY KEY REFERENCES users (id),
    name                                  VARCHAR(200),
    email                                 VARCHAR(320),
    birth_date                            DATE,
    sex                                   VARCHAR(16),
    height_cm                             NUMERIC(5, 1),
    activity_level                        VARCHAR(16),
    main_goal                             VARCHAR(16),
    weight_unit                           VARCHAR(8)   NOT NULL DEFAULT 'KG',
    height_unit                           VARCHAR(8)   NOT NULL DEFAULT 'CM',
    distance_unit                         VARCHAR(8)   NOT NULL DEFAULT 'KM',
    energy_unit                           VARCHAR(8)   NOT NULL DEFAULT 'KCAL',
    caloric_deficit_kcal                  NUMERIC(7, 1),
    protein_target_g                      NUMERIC(6, 1),
    daily_water_ml                        NUMERIC(7, 1),
    theme_mode                            VARCHAR(8)   NOT NULL DEFAULT 'DARK',
    onboarding_profile_name               VARCHAR(200) NOT NULL DEFAULT '',
    onboarding_profile_birth_date         VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_profile_sex                VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_profile_height_cm          VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_metrics_choice             VARCHAR(16),
    onboarding_metrics_saved              BOOLEAN      NOT NULL DEFAULT FALSE,
    onboarding_goal_selected              VARCHAR(16),
    onboarding_training_days              TEXT         NOT NULL DEFAULT '',
    onboarding_equipment_items            TEXT         NOT NULL DEFAULT '',
    onboarding_nutrition_preference       VARCHAR(500) NOT NULL DEFAULT '',
    onboarding_nutrition_restrictions     VARCHAR(500) NOT NULL DEFAULT '',
    first_run_completed                   BOOLEAN      NOT NULL DEFAULT FALSE,
    baseline_weight_kg                    NUMERIC(5, 1),
    baseline_body_fat_pct                 NUMERIC(4, 1),
    baseline_bmi                          NUMERIC(4, 1),
    base_calories_kcal                    NUMERIC(6, 1),
    body_fat_target_min_pct               NUMERIC(4, 1),
    body_fat_target_max_pct               NUMERIC(4, 1),
    weight_target_min_kg                  NUMERIC(5, 1),
    weight_target_max_kg                  NUMERIC(5, 1),
    fat_target_g                          NUMERIC(6, 1),
    carbs_target_g                        NUMERIC(6, 1)
);
INSERT INTO user_profile_v2
SELECT
    '00000000-0000-0000-0000-000000000000',
    name, email, birth_date, sex, height_cm, activity_level, main_goal,
    weight_unit, height_unit, distance_unit, energy_unit,
    caloric_deficit_kcal, protein_target_g, daily_water_ml, theme_mode,
    onboarding_profile_name, onboarding_profile_birth_date, onboarding_profile_sex,
    onboarding_profile_height_cm, onboarding_metrics_choice, onboarding_metrics_saved,
    onboarding_goal_selected, onboarding_training_days, onboarding_equipment_items,
    onboarding_nutrition_preference, onboarding_nutrition_restrictions, first_run_completed,
    baseline_weight_kg, baseline_body_fat_pct, baseline_bmi,
    base_calories_kcal, body_fat_target_min_pct, body_fat_target_max_pct,
    weight_target_min_kg, weight_target_max_kg, fat_target_g, carbs_target_g
FROM user_profile;
DROP TABLE user_profile;
ALTER TABLE user_profile_v2 RENAME TO user_profile;

-- ---------------------------------------------------------------------------------------------
-- integration_connection (V12 + V16's last_sync_duplicates_skipped column) -- owner_id was part of
-- the composite PK (owner_id, provider).
CREATE TABLE integration_connection_v2 (
    user_id                   UUID NOT NULL REFERENCES users (id),
    provider                  VARCHAR(32) NOT NULL,
    status                    VARCHAR(16) NOT NULL DEFAULT 'DISCONNECTED',
    connected_at              TIMESTAMP WITH TIME ZONE,
    last_sync_at              TIMESTAMP WITH TIME ZONE,
    last_sync_result          VARCHAR(16),
    last_sync_imported_count  INTEGER,
    last_sync_message         VARCHAR(500),
    last_sync_duplicates_skipped INTEGER,
    PRIMARY KEY (user_id, provider)
);
INSERT INTO integration_connection_v2
SELECT
    '00000000-0000-0000-0000-000000000000',
    provider, status, connected_at, last_sync_at,
    last_sync_result, last_sync_imported_count, last_sync_message, last_sync_duplicates_skipped
FROM integration_connection;
DROP TABLE integration_connection;
ALTER TABLE integration_connection_v2 RENAME TO integration_connection;

-- ---------------------------------------------------------------------------------------------
-- integration_token (V15) -- owner_id was part of the composite PK (owner_id, provider).
CREATE TABLE integration_token_v2 (
    user_id                  UUID NOT NULL REFERENCES users (id),
    provider                 VARCHAR(32) NOT NULL,
    access_token_ciphertext  BYTEA NOT NULL,
    access_token_nonce       BYTEA NOT NULL,
    refresh_token_ciphertext BYTEA NOT NULL,
    refresh_token_nonce      BYTEA NOT NULL,
    access_token_expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, provider)
);
INSERT INTO integration_token_v2
SELECT
    '00000000-0000-0000-0000-000000000000',
    provider, access_token_ciphertext, access_token_nonce,
    refresh_token_ciphertext, refresh_token_nonce, access_token_expires_at, updated_at
FROM integration_token;
DROP TABLE integration_token;
ALTER TABLE integration_token_v2 RENAME TO integration_token;

-- ---------------------------------------------------------------------------------------------
-- integration_oauth_state (V15) -- owner_id was part of the composite PK (owner_id, provider).
CREATE TABLE integration_oauth_state_v2 (
    user_id        UUID NOT NULL REFERENCES users (id),
    provider       VARCHAR(32) NOT NULL,
    state          VARCHAR(128) NOT NULL,
    code_verifier  VARCHAR(128) NOT NULL,
    code_challenge VARCHAR(128) NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, provider)
);
INSERT INTO integration_oauth_state_v2
SELECT
    '00000000-0000-0000-0000-000000000000',
    provider, state, code_verifier, code_challenge, expires_at
FROM integration_oauth_state;
DROP TABLE integration_oauth_state;
ALTER TABLE integration_oauth_state_v2 RENAME TO integration_oauth_state;

-- ---------------------------------------------------------------------------------------------
-- integration_measure_marker (V16) -- owner_id was part of the composite PK
-- (owner_id, provider, grpid).
CREATE TABLE integration_measure_marker_v2 (
    user_id     UUID NOT NULL REFERENCES users (id),
    provider    VARCHAR(32) NOT NULL,
    grpid       BIGINT NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, provider, grpid)
);
INSERT INTO integration_measure_marker_v2
SELECT '00000000-0000-0000-0000-000000000000', provider, grpid, imported_at
FROM integration_measure_marker;
DROP TABLE integration_measure_marker;
ALTER TABLE integration_measure_marker_v2 RENAME TO integration_measure_marker;

-- ---------------------------------------------------------------------------------------------
-- earned_achievement (V18) -- owner_id was part of the composite PK (owner_id, achievement_id).
CREATE TABLE earned_achievement_v2 (
    user_id        UUID NOT NULL REFERENCES users (id),
    achievement_id VARCHAR(64) NOT NULL,
    earned_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, achievement_id)
);
INSERT INTO earned_achievement_v2
SELECT '00000000-0000-0000-0000-000000000000', achievement_id, earned_at
FROM earned_achievement;
DROP TABLE earned_achievement;
ALTER TABLE earned_achievement_v2 RENAME TO earned_achievement;
