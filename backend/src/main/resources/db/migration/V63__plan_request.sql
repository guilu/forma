-- The plan request: a durable, structured record of what somebody asked a plan to do for them
-- (ADR-015). One row per request, keyed to the account that made it.
--
-- Not more columns on user_profile: that table's write path is full-replace with no history
-- (ADR-015 Context, point 2), so a second request would silently erase the first, and a request is
-- precisely the thing you need to be able to read back months later to explain why a plan says what
-- it says. Not plan_lead either: that table has no user_id at all, deliberately models an
-- anonymous funnel lead (V61: "un lead no es una cuenta"), and carries marketing-consent columns
-- that have nothing to do with a signed-in user asking for a plan. Both alternatives were rejected
-- by the product owner before ADR-015 was written.
--
-- WHY EVERY INPUT IS FROZEN ON THE ROW, NOT RE-READ LATER
--
-- sex/age/weight/height/activity_level/main_goal/plan_objective/plan_kcal are copied here from
-- user_profile and body_measurement at request time and never re-derived. The user's weight moves,
-- their profile changes; the request has to stay comparable against the numbers that produced the
-- plan they got, the same reasoning V61 already applied to plan_lead.plan_kcal. plan_kcal itself is
-- request AUDIT, not a sixth target: nothing renders it and nothing compares against it at read
-- time (ADR-015 decision 7).
--
-- ONE OPEN REQUEST PER USER, PORTABLY (ADR-015 decision 4)
--
-- open_marker is the same nullable-sentinel trick nutrition_plan.active_marker (V53) and
-- food_serving.default_marker (V49) already use: '1' while the row is PENDING or GENERATING, NULL
-- once it is terminal. SQL compares NULLs as distinct on both H2 and PostgreSQL, so
-- UNIQUE(user_id, open_marker) admits any number of finished requests and at most one open one —
-- ADR-011's portable substitute for a PostgreSQL partial unique index, which H2's CREATE INDEX
-- cannot parse a WHERE clause for.
--
-- The paired CHECK below is written open_marker IS NOT NULL, never open_marker = '1'. V53 found and
-- documented the hole in the latter form: for a row in an open status with a NULL marker, that
-- comparison evaluates to UNKNOWN rather than FALSE, the whole OR comes out UNKNOWN, and a CHECK
-- constraint ACCEPTS unknown — letting through exactly the row the unique index cannot constrain on
-- its own. IS NOT NULL is never UNKNOWN, and status is NOT NULL, so this form is two-valued
-- throughout.
--
-- WHY THE FK TO users IS ON DELETE CASCADE
--
-- A plan_request holds no special-category health data (ADR-015 decision 12: no allergies, no
-- pathologies, no injuries, ever) and has no declared retention period of its own, unlike plan_lead
-- (V61, twelve months). It belongs to an account and explains a plan that account may still be
-- following, so it lives and dies with the account — matching plan_acceptance's FK (V58).
CREATE TABLE plan_request (
    id                      UUID                     PRIMARY KEY,
    user_id                 UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- PENDING -> GENERATING -> READY | FAILED (domain/PlanRequestStatus). Terminal states are
    -- terminal: a retry is a new row, never a resurrected one (ADR-015 decision 2).
    status                  VARCHAR(16)              NOT NULL DEFAULT 'PENDING',
    open_marker             CHAR(1),

    -- Which shape of the two cross-boundary payloads this row spoke (ADR-015 decision 13). FORMA
    -- has no DTO versioning scheme anywhere else — every other contract lives entirely inside this
    -- repository and ships in one deployment. The agent is the first consumer FORMA does not
    -- deploy, so the two sides can disagree on some day nobody plans, and a payload that cannot say
    -- which contract it speaks fails that day by being misread rather than rejected.
    contract_version        VARCHAR(16)              NOT NULL,
    -- Which catalog snapshot travelled with the request. A separate axis from contract_version on
    -- purpose: this is data drift (a food was added), that is shape drift (a field changed), and
    -- conflating them would make the former look like the latter.
    catalog_version         VARCHAR(32),

    -- Frozen inputs (domain/Sex, domain/ActivityLevel, domain/MainGoal, domain/PlanObjective), read
    -- from user_profile and the newest body_measurement at request time. See the header comment for
    -- why these are copied rather than joined at read time.
    sex                     VARCHAR(16)              NOT NULL,
    age_years               INTEGER                  NOT NULL,
    weight_kg               NUMERIC(5, 1)            NOT NULL,
    height_cm               NUMERIC(5, 1)            NOT NULL,
    activity_level          VARCHAR(32)              NOT NULL,

    -- Two objective vocabularies, both carried, because they answer different questions and only
    -- one of them is arithmetic (ADR-015 decision 5, amended). main_goal is the profile's standing
    -- answer — what an accepted plan later writes back to nutrition_plan.objective, in the
    -- vocabulary that column already speaks (V53). plan_objective is this plan's clinical
    -- objective, and the only one of the two whose factor() is what EnergyRequirement.of
    -- multiplies by. It is never derived from main_goal: the wizard asks it explicitly, through
    -- domain/PlanDirection, precisely because deriving it silently was the hidden-decision hole
    -- ADR-015 decision 5 was rewritten to close. See the ADR's Open Points for the full reasoning.
    main_goal               VARCHAR(16)              NOT NULL,
    plan_objective          VARCHAR(32)              NOT NULL,

    -- Both weekday forms, because both are needed (ADR-015 decision 6): nutrition_plan_day's
    -- day_type would put the wrong kind of day on the wrong weekday if only the count travelled.
    -- training_weekdays is a CSV of java.time.DayOfWeek names, matching the onboarding_equipment_
    -- items TEXT-CSV precedent (V8) and ADR-011's no-JSONB rule; NULL means nobody said, which is
    -- different from an empty list (the wizard step is optional). training_days_per_week allows 0
    -- where the public funnel's PlanDraftRequest requires 3 (bean-validated @Min(3)): the funnel
    -- asks somebody who already filled in four screens, the wizard's training-days step says
    -- "(opcional)" on screen.
    training_days_per_week INTEGER                  NOT NULL,
    training_weekdays      VARCHAR(64),
    -- CSV of domain/TrainingEquipment. Carried from day one, unused by this slice: it is a training
    -- input and there is no training generator yet (ADR-015 decision 11) — the request is the
    -- input, the nutrition plan is only one of its outputs.
    equipment               VARCHAR(255),

    meals_per_day           INTEGER                  NOT NULL,
    -- domain/DietPattern (exclusion rule) and domain/CuisineStyle (cuisine), genuinely two axes:
    -- a vegan Mediterranean diet is not a contradiction and one column cannot say it (decision 6).
    -- Both UNSPECIFIED-able and both NOT NULL: "nobody said" is a stored answer, not a missing row.
    diet_pattern             VARCHAR(32)              NOT NULL,
    cuisine_style            VARCHAR(32)              NOT NULL,

    -- Mifflin-St Jeor x activity factor x plan_objective factor (EnergyRequirement.of), computed
    -- once at request time and frozen (ADR-015 decision 7). Deliberately NOT a sixth target: it is
    -- request audit, and the day a screen starts reading it as one is the day to re-check this
    -- comment.
    plan_kcal                INTEGER                  NOT NULL,
    target_protein_g         NUMERIC(6, 1),
    target_carbs_g           NUMERIC(6, 1),
    target_fat_g             NUMERIC(6, 1),

    -- Audit of the two HTTP bodies that cross the boundary. TEXT, not JSONB (ADR-011: H2 has none,
    -- and nothing here queries inside either blob — read whole or not at all, exactly the
    -- nutrition_plan.generation_metadata precedent (V53)).
    request_payload          TEXT,
    -- The section-11 tolerance audit result, written by the ingest (ADR-015 decision 9). NULL until
    -- then; a drift never rejects the plan, it is recorded here and the plan is stored as DRAFT.
    validation_report        TEXT,

    -- NULL until READY, then the plan this request produced. The CHECK below is what makes "READY"
    -- mean something: a READY row without a plan would be a status lying about what happened.
    nutrition_plan_id        UUID                     REFERENCES nutrition_plan (id),

    -- TIMEOUT / UNREACHABLE / REJECTED / MALFORMED, written by the dispatcher or the sweeper
    -- (ADR-015 decisions 3 and 9). Free-text VARCHAR rather than a CHECK-enforced vocabulary in this
    -- slice: the dispatcher and ingest that assign these values are ADR-015 slice 6, out of scope
    -- here, and constraining a vocabulary before its only writer exists risks locking in a guess.
    failure_code              VARCHAR(32),
    -- A safe summary. Never a stack trace, never a secret (ADR-005, ADR-008) — the same rule
    -- adapter/withings keeps for a failed provider call.
    failure_detail            TEXT,
    -- Exists so adding retry later needs no migration (ADR-015 decision 10: "no retry, still" — a
    -- failed attempt becomes a visible FAILED row, not a silent second try).
    attempt_count             INTEGER                  NOT NULL DEFAULT 0,

    requested_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- When the row became GENERATING — what the timeout sweep reads to find a wedged request.
    dispatched_at             TIMESTAMP WITH TIME ZONE,
    completed_at              TIMESTAMP WITH TIME ZONE,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_status CHECK (
  status IN ('PENDING', 'GENERATING', 'READY', 'FAILED')
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_marker_value CHECK (
  open_marker IS NULL OR open_marker = '1'
);

-- Two-valued throughout: status is NOT NULL and IS NOT NULL is never UNKNOWN. See the header
-- comment — this is the exact hole V53 found and closed for nutrition_plan.active_marker, written
-- the same way here on purpose.
ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_open_marker CHECK (
  (status IN ('PENDING', 'GENERATING') AND open_marker IS NOT NULL)
  OR (status NOT IN ('PENDING', 'GENERATING') AND open_marker IS NULL)
);

-- A READY request always points at a stored plan; the status and the plan are written in the same
-- transaction (ADR-015 decision 2), so this CHECK can never be the thing that fails in practice —
-- it exists so a bug that tried to write READY without one is rejected by the database rather than
-- discovered by a screen with nothing to show.
ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_ready_has_plan CHECK (
  status <> 'READY' OR nutrition_plan_id IS NOT NULL
);

-- Mirrors PlanDraftRequest's bean validation for the same field, so the funnel and the wizard agree
-- on what a plausible person looks like.
ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_age CHECK (age_years BETWEEN 14 AND 120);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_body CHECK (
  weight_kg > 0 AND height_cm > 0
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_meals CHECK (
  meals_per_day BETWEEN 3 AND 6
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_training_days CHECK (
  training_days_per_week BETWEEN 0 AND 7
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_kcal CHECK (plan_kcal > 0);

CREATE UNIQUE INDEX ux_plan_request_user_open ON plan_request (user_id, open_marker);

-- What a "your requests" screen would scan by, if one is ever built (ADR-015 Open Points).
CREATE INDEX ix_plan_request_user_requested ON plan_request (user_id, requested_at);

-- The dispatcher's and the timeout sweep's scan (ADR-015 decision 3): both claim by status.
CREATE INDEX ix_plan_request_status ON plan_request (status);
