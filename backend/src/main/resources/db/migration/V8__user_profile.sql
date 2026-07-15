-- Foundation: user_profile table (FOR-107).
--
-- Persists the single-user profile/preferences/onboarding aggregate. Additive migration on top of
-- V7 (ADR-003) — earlier migrations are untouched.
--
-- One combined table backs the whole UserProfile aggregate (profile fields + unit preferences +
-- default objectives + theme + onboarding draft + firstRunCompleted), rather than three separate
-- tables, per spec FOR-107 Open Questions ("recommend keeping the API surface as one read model
-- even if persistence is split... or a single combined table if the implementer judges the split
-- unnecessary for MVP scale"): at MVP single-row scale a join buys nothing. owner_id is the primary
-- key rather than a generated id — there is exactly one row per account (ADR-002 single-user MVP;
-- the column exists so authorization can be layered on later without a schema rewrite).
--
-- Preference/theme columns carry NOT NULL DEFAULTs matching the domain's own defaults (metric
-- units, dark theme) so a row inserted with only some columns set still reads back consistently.
-- Onboarding draft columns default to blank/false, mirroring OnboardingAnswers.EMPTY.
CREATE TABLE user_profile (
    owner_id                            VARCHAR(64) PRIMARY KEY,
    name                                 VARCHAR(200),
    email                                VARCHAR(320),
    birth_date                           DATE,
    sex                                  VARCHAR(16),
    height_cm                            NUMERIC(5, 1),
    activity_level                       VARCHAR(16),
    main_goal                            VARCHAR(16),
    weight_unit                          VARCHAR(8)   NOT NULL DEFAULT 'KG',
    height_unit                          VARCHAR(8)   NOT NULL DEFAULT 'CM',
    distance_unit                        VARCHAR(8)   NOT NULL DEFAULT 'KM',
    energy_unit                          VARCHAR(8)   NOT NULL DEFAULT 'KCAL',
    caloric_deficit_kcal                 NUMERIC(7, 1),
    protein_target_g                     NUMERIC(6, 1),
    daily_water_ml                       NUMERIC(7, 1),
    theme_mode                           VARCHAR(8)   NOT NULL DEFAULT 'DARK',
    onboarding_profile_name              VARCHAR(200) NOT NULL DEFAULT '',
    onboarding_profile_birth_date        VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_profile_sex               VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_profile_height_cm         VARCHAR(32)  NOT NULL DEFAULT '',
    onboarding_metrics_choice            VARCHAR(16),
    onboarding_metrics_saved             BOOLEAN      NOT NULL DEFAULT FALSE,
    onboarding_goal_selected             VARCHAR(16),
    onboarding_training_days             TEXT         NOT NULL DEFAULT '',
    onboarding_equipment_items           TEXT         NOT NULL DEFAULT '',
    onboarding_nutrition_preference      VARCHAR(500) NOT NULL DEFAULT '',
    onboarding_nutrition_restrictions    VARCHAR(500) NOT NULL DEFAULT '',
    first_run_completed                  BOOLEAN      NOT NULL DEFAULT FALSE
);
